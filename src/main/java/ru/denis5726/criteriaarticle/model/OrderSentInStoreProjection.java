package ru.denis5726.criteriaarticle.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderSentInStoreProjection {
    private UUID id;
    private ZonedDateTime createdAt;
    private BigDecimal totalPrice;
}
