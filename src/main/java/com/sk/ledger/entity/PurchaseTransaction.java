package com.sk.ledger.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Since JPA needs to map data from the database into this object, it needs a no-args constructor.
 * However,we avoid @Data because it can cause performance issues with JPA's dirty checking and circular dependencies.
 */
@Entity
@Table(name = "purchases")
@Getter
@Setter
@NoArgsConstructor // Required by JPA
@AllArgsConstructor // Required by @Builder
@Builder
public class PurchaseTransaction {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID idempotencyKey;

    @Column(length = 50)
    private String description;

    @Column(nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime transactionDate;

    private BigDecimal amountUsd;

}
