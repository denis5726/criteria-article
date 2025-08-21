package ru.denis5726.criteriaarticle.repository.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaCteCriteria;
import org.springframework.stereotype.Repository;
import ru.denis5726.criteriaarticle.entity.Category;
import ru.denis5726.criteriaarticle.entity.Category_;
import ru.denis5726.criteriaarticle.entity.Order;
import ru.denis5726.criteriaarticle.entity.OrderItem_;
import ru.denis5726.criteriaarticle.entity.OrderStatusHistory_;
import ru.denis5726.criteriaarticle.entity.Order_;
import ru.denis5726.criteriaarticle.entity.Product_;
import ru.denis5726.criteriaarticle.model.OrderSentInStoreProjection;
import ru.denis5726.criteriaarticle.model.OrderShortInfoProjection;
import ru.denis5726.criteriaarticle.model.OrderStoreStatisticProjection;
import ru.denis5726.criteriaarticle.model.OrderWithTotalPriceProjection;
import ru.denis5726.criteriaarticle.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

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
        // Вынесем выражения подсчёта количества уникальных заказов со статусом в отдельный метод
        // Выражения подсчёта количества с каждым статусом вынесем в отдельную переменную,
        // так как они понадобятся в нескольких местах
        final var completedCount = countDistinctOrderByStatus(cb, order, Order.Status.COMPLETED);
        final var canceledCount = countDistinctOrderByStatus(cb, order, Order.Status.CANCELED);
        final var rejectedCount = countDistinctOrderByStatus(cb, order, Order.Status.REJECTED);
        // Выражение подсчёта общей стоимости заказа тоже вынесем в отдельную переменную
        final var totalOrderPrice = cb.sum(
                cb.<BigDecimal>prod(product.get(Product_.PRICE), items.get(OrderItem_.QUANTITY))
        );

        query.multiselect(
                        // Для каждого столбца указываем явно тип и псевдоним, чтобы по нему потом достать значение
                        // из кортежа
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
                // Сортируем магазины по убыванию количества завершённых заказов
                .orderBy(cb.desc(cb.sum(cb.sum(completedCount, canceledCount), rejectedCount)));

        return entityManager.createQuery(query).getResultList().stream()
                .map(this::tupleToStatisticProjection)
                .toList();
    }

    @Override
    public List<OrderWithTotalPriceProjection> findOrderWithProductCategory(String categoryName) {
        final var cb = entityManager.unwrap(Session.class).getCriteriaBuilder();
        final var query = cb.createQuery(OrderWithTotalPriceProjection.class);

        // Создаём рекурсивный CTE all_category. Для его создания нужен базовый запрос и
        // функция создания рекурсивного запроса из прошлого запроса. Создаём каждый запрос в отдельном методе
        final var allCategoryCte = query.withRecursiveUnionAll(
                getAllCategoryCteBaseQuery(categoryName, cb),
                getAllCategoryRecursiveQueryProducer(cb)
        );
        // Создаём query CTE all_order в отдельном методе
        final var allOrderCteQuery = getAllOrderCteQuery(cb, allCategoryCte);

        // Создаём объект CTE all_order из query
        final var allOrderCte = query.with(allOrderCteQuery);
        // Не нашёл способа делать JOIN с CTE, поэтому делаем "декартово" произведение, а условие ON пишем в WHERE
        // Для БД оба способа эквивалентны и не должны отличаться по производительности
        final var allOrderRoot = query.from(allOrderCte);
        final var order = query.from(Order.class);
        final var orderItem = order.join(Order_.ITEMS);
        final var product = orderItem.join(OrderItem_.PRODUCT);

        query
                .select(cb.construct(
                        OrderWithTotalPriceProjection.class,
                        // Пример использования составного Selection с помощью вложенного cb.construct
                        cb.construct(
                                OrderShortInfoProjection.class,
                                order.get(Order_.ID),
                                order.get(Order_.STORE_ID),
                                order.get(Order_.STATUS)
                        ),
                        cb.sum(cb.prod(orderItem.get(OrderItem_.QUANTITY), product.get(Product_.PRICE)))
                ))
                // То, что обычно в WHERE
                .where(cb.equal(
                        order.get(Order_.ID),
                        allOrderRoot.get(Order_.ID)
                ))
                .groupBy(order);

        return entityManager.createQuery(query).getResultList();
    }

    private JpaCriteriaQuery<Tuple> getAllCategoryCteBaseQuery(String categoryName, HibernateCriteriaBuilder cb) {
        final var allCategoryCteBaseQuery = cb.createTupleQuery();
        final var allCategoryCteBaseCategory = allCategoryCteBaseQuery.from(Category.class);
        // JOIN таблицы на саму себя по связи parent
        final var allCategoryCteBaseParentCategory = allCategoryCteBaseCategory.join(Category_.PARENT);
        // Выбираем id и name той category, у которой родитель с именем categoryName
        return allCategoryCteBaseQuery
                .multiselect(
                        allCategoryCteBaseCategory.get(Category_.ID).as(UUID.class).alias(Category_.ID),
                        allCategoryCteBaseCategory.get(Category_.NAME).as(String.class).alias(Category_.NAME)
                )
                .where(cb.equal(allCategoryCteBaseParentCategory.get(Category_.NAME), categoryName));
    }

    private JpaCriteriaQuery<Tuple> getAllOrderCteQuery(
            HibernateCriteriaBuilder cb,
            JpaCteCriteria<Tuple> allCategoryCte
    ) {
        final var allOrderCteQuery = cb.createTupleQuery();
        final var allOrderCteOrder = allOrderCteQuery.from(Order.class);
        final var allOrderCteOrderItem = allOrderCteOrder.join(Order_.ITEMS);
        final var allOrderCteProduct = allOrderCteOrderItem.join(OrderItem_.PRODUCT);
        // Опять же заменяем JOIN на "декартово" с WHERE
        final var allOrderCteAllCategory = allOrderCteQuery.from(allCategoryCte);
        return allOrderCteQuery
                .distinct(true)
                .multiselect(
                        allOrderCteOrder.get(Order_.ID).as(UUID.class).alias(Order_.ID)
                )
                .where(cb.equal(
                        // Условие ON при JOIN категории с продуктом
                        allOrderCteProduct.get(Product_.CATEGORY).get(Category_.ID),
                        allOrderCteAllCategory.get(Category_.ID)
                ));
    }

    private Function<JpaCteCriteria<Tuple>, AbstractQuery<Tuple>> getAllCategoryRecursiveQueryProducer(
            HibernateCriteriaBuilder cb
    ) {
        return previousCte -> {
            final var allCategoryCteRecursiveQuery = cb.createTupleQuery();
            // Опять же заменяем JOIN на "декартово" с WHERE
            final var allCategoryCteRecursiveCategory = allCategoryCteRecursiveQuery.from(Category.class);
            final var allCategoryCteRecursivePreviousCte = allCategoryCteRecursiveQuery.from(previousCte);
            allCategoryCteRecursiveQuery
                    .multiselect(
                            allCategoryCteRecursiveCategory.get(Category_.ID).as(UUID.class).alias(Category_.ID),
                            allCategoryCteRecursiveCategory.get(Category_.NAME).as(String.class).alias(Category_.NAME)
                    )
                    .where(cb.equal(
                            // id предыдущей (выше по иерархии) категории должно являться id родителя новой
                            allCategoryCteRecursivePreviousCte.get(Category_.ID),
                            allCategoryCteRecursiveCategory.get(Category_.PARENT).get(Category_.ID)
                    ));

            return allCategoryCteRecursiveQuery;
        };
    }

    private Expression<Long> countDistinctOrderByStatus(
            CriteriaBuilder cb,
            Root<Order> order,
            Order.Status status
    ) {
        // Так записываем COUNT(DISTINCT CASE WHEN order.status = status THEN order.id END)
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
                // Вот так можно извлекать значения из кортежа, но есть и другие способы,
                // например, по номеру столбца
                tuple.get("storeId", UUID.class),
                tuple.get("completed", Long.class),
                tuple.get("canceled", Long.class),
                tuple.get("rejected", Long.class)
        );
    }
}
