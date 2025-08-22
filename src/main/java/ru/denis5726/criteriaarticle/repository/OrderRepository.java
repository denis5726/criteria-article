package ru.denis5726.criteriaarticle.repository;

import ru.denis5726.criteriaarticle.entity.Order;
import ru.denis5726.criteriaarticle.model.OrderDayStatisticProjection;
import ru.denis5726.criteriaarticle.model.OrderSentInStoreProjection;
import ru.denis5726.criteriaarticle.model.OrderShortInfoProjection;
import ru.denis5726.criteriaarticle.model.OrderStoreStatisticProjection;
import ru.denis5726.criteriaarticle.model.OrderWithTotalPriceProjection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface OrderRepository {

    List<OrderSentInStoreProjection> findSentInStoreOrdersByStoreId(UUID storeId);

    List<OrderStoreStatisticProjection> findStoreStatistic(BigDecimal lowerBound, BigDecimal upperBound);

    List<OrderShortInfoProjection> findOrderWithProductInCategories(List<String> categoryNames);

    List<OrderWithTotalPriceProjection> findOrderWithProductCategory(String categoryName);

    List<OrderDayStatisticProjection> findOrderDayStatistic(LocalDate startDate, LocalDate endDate);
}
