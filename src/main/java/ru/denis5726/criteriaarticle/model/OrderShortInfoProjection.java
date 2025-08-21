package ru.denis5726.criteriaarticle.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.denis5726.criteriaarticle.entity.Order;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderShortInfoProjection {
    private UUID id;
    private UUID storeId;
    private Order.Status status;
}
