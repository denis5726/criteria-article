package ru.denis5726.criteriaarticle.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderWithTotalPriceProjection {
    private OrderShortInfoProjection shortInfo;
    private BigDecimal totalPrice;
}
