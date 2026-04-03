package com.sk.ledger.model;

import com.sk.ledger.entity.PurchaseTransaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data                // Generates getters, setters, equals, hashCode, and toString
@Builder             // Allows for TransactionResult.builder()...build()
@NoArgsConstructor   // Required for some serialization frameworks
@AllArgsConstructor  // Required for the Builder to work
public class TransactionResult {

    private PurchaseTransaction transaction;
    private BigDecimal exchangeRate;

}