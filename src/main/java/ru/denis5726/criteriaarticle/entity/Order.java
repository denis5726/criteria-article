package ru.denis5726.criteriaarticle.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "`order`")
@EntityListeners(AuditingEntityListener.class)
public class Order {
    @Id
    @GeneratedValue
    private UUID id;
    private UUID storeId;
    private UUID customerId;
    @Enumerated(EnumType.STRING)
    private Status status;
    @OneToMany(mappedBy = "order")
    private List<OrderStatusHistory> historyRecords;
    @OneToMany(mappedBy = "order")
    private List<OrderItem> items;
    private ZonedDateTime finishedAt;
    @CreatedDate
    private ZonedDateTime createdAt;

    public enum Status {
        NEW,
        SENT_TO_STORE,
        RECEIVED_IN_STORE,
        IN_PROCESSING,
        PROCESSED,
        CANCELED,
        REJECTED,
        COMPLETED
    }
}
