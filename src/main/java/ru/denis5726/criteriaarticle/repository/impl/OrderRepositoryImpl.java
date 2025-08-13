package ru.denis5726.criteriaarticle.repository.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;
import ru.denis5726.criteriaarticle.entity.Order;
import ru.denis5726.criteriaarticle.entity.OrderItem_;
import ru.denis5726.criteriaarticle.entity.OrderStatusHistory_;
import ru.denis5726.criteriaarticle.entity.Order_;
import ru.denis5726.criteriaarticle.entity.Product_;
import ru.denis5726.criteriaarticle.model.OrderSentInStoreProjection;
import ru.denis5726.criteriaarticle.model.OrderStoreStatisticProjection;
import ru.denis5726.criteriaarticle.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {
    private final EntityManager entityManager;

    @Override
    public List<OrderSentInStoreProjection> findSentInStoreOrdersByStoreId(UUID storeId) {
        // Вот так можно удобно приводить EntityManager к Session
        final var cb = entityManager.unwrap(Session.class).getCriteriaBuilder();
        final var query = cb.createQuery(OrderSentInStoreProjection.class);
        final var order = query.from(Order.class);
        // Классы с названием сущности и "_" в конце сгенерированы с помощью hibernate-jpamodelgen
        // и содержат константы с названиями всех полей сущности
        final var historyRecords = order.join(Order_.HISTORY_RECORDS, JoinType.INNER);
        final var items = order.join(Order_.ITEMS, JoinType.INNER);
        final var product = items.join(OrderItem_.PRODUCT, JoinType.INNER);

        query
                .select(cb.construct(
                        OrderSentInStoreProjection.class,
                        order.get(Order_.ID),
                        order.get(Order_.CREATED_AT),
                        cb.sum(
                                // Метод HibernateCriteriaBuilder
                                cb.prod(product.get(Product_.PRICE), items.get(OrderItem_.QUANTITY))
                        )
                ))
                .where(
                        cb.and(
                                // Оборачиваем Order.Status.SENT_TO_STORE в cb.literal, чтобы передавать
                                // 'SENT_TO_STORE' не JDBC-параметром (?), а константой
                                cb.equal(historyRecords.get(OrderStatusHistory_.STATUS), cb.literal(Order.Status.SENT_TO_STORE)),
                                cb.equal(order.get(Order_.STORE_ID), storeId)
                        )

                )
                .groupBy(order.get(Order_.ID), order.get(Order_.CREATED_AT))
                .orderBy(cb.desc(order.get(Order_.CREATED_AT)));

        return entityManager.createQuery(query).getResultList();
    }

    @Override
    public List<OrderStoreStatisticProjection> findStoreStatistic(BigDecimal lowerBound, BigDecimal upperBound) {
        final var cb = entityManager.unwrap(Session.class).getCriteriaBuilder();
        final var query = cb.createTupleQuery();
        final var order = query.from(Order.class);
        final var items = order.join(Order_.ITEMS, JoinType.INNER);
        final var product = items.join(OrderItem_.PRODUCT, JoinType.INNER);
        final var completedCount = countDistinctOrderByStatus(cb, order, Order.Status.COMPLETED);
        final var canceledCount = countDistinctOrderByStatus(cb, order, Order.Status.CANCELED);
        final var rejectedCount = countDistinctOrderByStatus(cb, order, Order.Status.REJECTED);
        final var totalOrderPrice = cb.sum(
                cb.<BigDecimal>prod(product.get(Product_.PRICE), items.get(OrderItem_.QUANTITY))
        );

        query.multiselect(
                        order.get(Order_.STORE_ID).as(UUID.class).alias("storeId"),
                        completedCount.as(Long.class).alias("completed"),
                        canceledCount.as(Long.class).alias("canceled"),
                        rejectedCount.as(Long.class).alias("rejected")
                )
                .groupBy(order.get(Order_.STORE_ID))
                .having(
                        cb.and(
                                cb.greaterThan(
                                        totalOrderPrice,
                                        lowerBound
                                ),
                                cb.lessThan(
                                        totalOrderPrice,
                                        upperBound
                                )
                        )
                )
                .orderBy(cb.desc(cb.sum(cb.sum(completedCount, canceledCount), rejectedCount)));

        return entityManager.createQuery(query).getResultList().stream()
                .map(this::tupleToStatisticProjection)
                .toList();
    }

    private Expression<Long> countDistinctOrderByStatus(
            CriteriaBuilder cb,
            Root<Order> order,
            Order.Status status
    ) {
        return cb.countDistinct(
                cb.selectCase()
                        .when(
                                cb.equal(order.get(Order_.STATUS), cb.literal(status)),
                                order.get(Order_.ID)
                        )
        );
    }

    private OrderStoreStatisticProjection tupleToStatisticProjection(Tuple tuple) {
        return new OrderStoreStatisticProjection(
                tuple.get("storeId", UUID.class),
                tuple.get("completed", Long.class),
                tuple.get("canceled", Long.class),
                tuple.get("rejected", Long.class)
        );
    }
}
