package ru.denis5726.criteriaarticle.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStoreStatisticProjection {
    private UUID storeId;
    private Long completedCount;
    private Long canceledCount;
    private Long rejectedCount;
}
