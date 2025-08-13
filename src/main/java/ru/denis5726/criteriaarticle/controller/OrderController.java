package ru.denis5726.criteriaarticle.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.denis5726.criteriaarticle.model.OrderSentInStoreProjection;
import ru.denis5726.criteriaarticle.model.OrderStoreStatisticProjection;
import ru.denis5726.criteriaarticle.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
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
}
