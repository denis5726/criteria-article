package ru.denis5726.criteriaarticle.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.denis5726.criteriaarticle.entity.Order;
import ru.denis5726.criteriaarticle.model.OrderDayStatisticProjection;
import ru.denis5726.criteriaarticle.model.OrderSentInStoreProjection;
import ru.denis5726.criteriaarticle.model.OrderShortInfoProjection;
import ru.denis5726.criteriaarticle.model.OrderStoreStatisticProjection;
import ru.denis5726.criteriaarticle.model.OrderWithTotalPriceProjection;
import ru.denis5726.criteriaarticle.repository.OrderRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderController {
    private final OrderRepository repository;

    @GetMapping("/sentInStoreOrders")
    public List<OrderSentInStoreProjection> findSentInStoreOrdersByStoreId(@RequestParam UUID storeId) {
        return repository.findSentInStoreOrdersByStoreId(storeId);
    }

    @GetMapping("/storeStatistic")
    public List<OrderStoreStatisticProjection> findStoreStatistic(
            @RequestParam BigDecimal lowerBound,
            @RequestParam BigDecimal upperBound
    ) {
        return repository.findStoreStatistic(lowerBound, upperBound);
    }

    @GetMapping("/ordersWithProductInCategories")
    public List<OrderShortInfoProjection> findOrderWithProductInCategories(
            @RequestParam(name = "categoryName") List<String> categoryNames
    ) {
        return repository.findOrderWithProductInCategories(categoryNames);
    }

    @GetMapping("/ordersWithProductCategory")
    public List<OrderWithTotalPriceProjection> findOrderWithProductCategory(
            @RequestParam String categoryName
    ) {
        return repository.findOrderWithProductCategory(categoryName);
    }

    @GetMapping("/orderDayStatistic")
    public List<OrderDayStatisticProjection> findOrderDayStatistic(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        return repository.findOrderDayStatistic(startDate, endDate);
    }
}
