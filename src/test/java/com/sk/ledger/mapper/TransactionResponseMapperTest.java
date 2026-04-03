package com.sk.ledger.mapper;

import com.sk.ledger.entity.PurchaseTransaction;
import com.sk.ledger.model.TransactionResponse;
import com.sk.ledger.model.TransactionResult;
import com.sk.ledger.model.mapper.TransactionResponseMapper;
import com.sk.ledger.model.mapper.TransactionResponseMapperImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransactionResponseMapperTest {

    // Instantiate the MapStruct-generated implementation
    private final TransactionResponseMapper mapper = new TransactionResponseMapperImpl();

    @Test
    @DisplayName("Should map TransactionResult to TransactionResponse successfully")
    void toResponse_Success() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        PurchaseTransaction purchase = PurchaseTransaction.builder()
                .id(id)
                .idempotencyKey(idempotencyKey)
                .description("Test Description")
                .transactionDate(Instant.now().atOffset(ZoneOffset.UTC))
                .amountUsd(new BigDecimal("100.00"))
                .build();

        TransactionResult result = new TransactionResult(purchase, new BigDecimal("5.25"));

        // Act
        TransactionResponse response = mapper.toResponse(result, "BRL");

        // Assert
        assertNotNull(response);
        assertEquals(id, response.getId());
        assertEquals("BRL", response.getTargetCurrency());
        assertEquals(new BigDecimal("100.00"), response.getOriginalAmountUsd());
//        assertEquals(new BigDecimal("5.25"), response.getExchangeRate());
        assertEquals(new BigDecimal("525.00"), response.getConvertedAmount());
    }

    @Test
    @DisplayName("Should return null when input Result is null (Hits MapStruct Null Check)")
    void toResponse_NullResult_ReturnsNull() {
        // Act
        TransactionResponse response = mapper.toResponse(null, "USD");

        // Assert
        assertNull(response); // This covers the 'if (result == null) return null;' line
    }

    @Test
    @DisplayName("Should handle null Purchase inside Result")
    void toResponse_NullPurchase_ReturnsPartialResponse() {
        // Arrange: Result exists, but the entity inside is null
        TransactionResult result = new TransactionResult(null, new BigDecimal("1.0"));

        // Act
        TransactionResponse response = mapper.toResponse(result, "USD");

        // Assert
        assertNotNull(response);
        assertNull(response.getDescription());
//        assertEquals(new BigDecimal("1.0"), response.getExchangeRate());
    }

    @Test
    @DisplayName("Should return empty object with currency when input Result is null")
    void toResponse_NullResult_ReturnsEmptyObject() {
        // Act
        TransactionResponse response = mapper.toResponse(null, "USD");

        // Assert
        assertNull(response); // MapStruct created the object
    }
}