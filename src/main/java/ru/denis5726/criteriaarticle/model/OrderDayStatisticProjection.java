package ru.denis5726.criteriaarticle.model;

import jakarta.persistence.metamodel.StaticMetamodel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDayStatisticProjection {
    public static final String DAY = "day";
    public static final String TOTAL_AMOUNT = "totalAmount";

    private LocalDate day;
    private BigDecimal totalAmount;
    private BigDecimal percentage;
    private BigDecimal diff;
}
