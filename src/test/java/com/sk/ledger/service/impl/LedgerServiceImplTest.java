package com.sk.ledger.service.impl;

import com.sk.ledger.entity.PurchaseTransaction;
import com.sk.ledger.exception.DuplicateTransactionException;
import com.sk.ledger.exception.TransactionNotFoundException;
import com.sk.ledger.model.TransactionRequest;
import com.sk.ledger.model.TransactionResult;
import com.sk.ledger.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerServiceImplTest {

    @Mock
    private TransactionRepository repository;

    @Mock
    private ExchangeRatesServiceImpl exchangeRatesClient;

    @InjectMocks
    private LedgerServiceImpl ledgerService;

    @Test
    @DisplayName("saveTransaction: Should save and return transaction with rounded amount")
    void saveTransaction_NewIdempotencyKey_SavesData() {
        // Arrange
        UUID key = UUID.randomUUID();
        TransactionRequest request = new TransactionRequest();
        request.setDescription("Coffee");
        // 5.555 with HALF_UP to 2 decimal places should be 5.56
        request.setAmountUsd(new BigDecimal("5.555"));
        request.setTransactionDate(Instant.now().atOffset(ZoneOffset.UTC));

        when(repository.findByIdempotencyKey(key)).thenReturn(Optional.empty());
        // Return the object that was passed into the save method
        when(repository.save(any(PurchaseTransaction.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        PurchaseTransaction result = ledgerService.saveTransaction(request, key);

        // Assert
        assertNotNull(result);
        assertEquals(0, new BigDecimal("5.56").compareTo(result.getAmountUsd()), "Amount should be rounded to 5.56");
        assertEquals(key, result.getIdempotencyKey());
        verify(repository, times(1)).save(any(PurchaseTransaction.class));
    }

    @Test
    @DisplayName("saveTransaction - Existing Key: Should throw DuplicateTransactionException")
    void saveTransaction_ExistingKey_ThrowsException() {
        // Arrange
        UUID key = UUID.randomUUID();
        TransactionRequest request = new TransactionRequest();
        request.setAmountUsd(BigDecimal.TEN);

        PurchaseTransaction existing = PurchaseTransaction.builder().idempotencyKey(key).build();
        when(repository.findByIdempotencyKey(key)).thenReturn(Optional.of(existing));

        // Act & Assert
        assertThrows(DuplicateTransactionException.class, () -> {
            ledgerService.saveTransaction(request, key);
        });

        verify(repository, never()).save(any());  // Verify save is not called because of the exception
    }

    @Test
    @DisplayName("getTransactionData - Found: Should return result with exchange rate")
    void getTransactionData_Found_ReturnsResult() {
        // Arrange
        UUID id = UUID.randomUUID();
        String currency = "EUR";
        OffsetDateTime date = Instant.now().atOffset(ZoneOffset.UTC).minusMonths(6);;
        PurchaseTransaction purchase = PurchaseTransaction.builder()
                .id(id)
                .transactionDate(date)
                .build();

        when(repository.findById(id)).thenReturn(Optional.of(purchase));
        when(exchangeRatesClient.getExchangeRate(currency, date)).thenReturn(new BigDecimal("0.92"));

        // Act
        TransactionResult result = ledgerService.getTransactionData(id, currency);

        // Assert
        assertEquals(new BigDecimal("0.92"), result.getExchangeRate());
        assertEquals(id, result.getTransaction().getId());
    }

    @Test
    @DisplayName("getTransactionData - Not Found: Should throw TransactionNotFoundException")
    void getTransactionData_NotFound_ThrowsTransactionNotFoundException() {
        // Arrange
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenThrow(TransactionNotFoundException.class);

        // Act &Assert
        assertThrows(TransactionNotFoundException.class, () -> {
            ledgerService.getTransactionData(id, "USD");
        });
        verifyNoInteractions(exchangeRatesClient); // API shouldn't be called if DB record is missing
    }
}