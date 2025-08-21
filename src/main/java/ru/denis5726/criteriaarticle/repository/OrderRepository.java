package ru.denis5726.criteriaarticle.repository;

import ru.denis5726.criteriaarticle.entity.Order;
import ru.denis5726.criteriaarticle.model.OrderSentInStoreProjection;
import ru.denis5726.criteriaarticle.model.OrderStoreStatisticProjection;
import ru.denis5726.criteriaarticle.model.OrderWithTotalPriceProjection;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface OrderRepository {

    List<OrderSentInStoreProjection> findSentInStoreOrdersByStoreId(UUID storeId);

    List<OrderStoreStatisticProjection> findStoreStatistic(BigDecimal lowerBound, BigDecimal upperBound);

    List<OrderWithTotalPriceProjection> findOrderWithProductCategory(String categoryName);
}
