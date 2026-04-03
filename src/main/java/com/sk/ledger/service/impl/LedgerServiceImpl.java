package com.sk.ledger.service.impl;

import com.sk.ledger.entity.PurchaseTransaction;
import com.sk.ledger.exception.DuplicateTransactionException;
import com.sk.ledger.exception.TransactionNotFoundException;
import com.sk.ledger.model.TransactionRequest;
import com.sk.ledger.model.TransactionResult;
import com.sk.ledger.repository.TransactionRepository;
import com.sk.ledger.service.ExchangeRatesService;
import com.sk.ledger.service.LedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j // Lombok logging
public class LedgerServiceImpl implements LedgerService {

    private final TransactionRepository repository;
    private final ExchangeRatesService exchangeRatesProvider;

    @Transactional
    public PurchaseTransaction saveTransaction(TransactionRequest request, UUID idempotencyKey) {
        // Check for existing idempotency key to avoid duplicates
        if (repository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            log.error("Duplicate idempotency key detected: {}", idempotencyKey);
            throw new DuplicateTransactionException("Transaction with key "
                    + idempotencyKey + " already exists.");
        }
        PurchaseTransaction purchase = PurchaseTransaction.builder()
                .idempotencyKey(idempotencyKey)
                .description(request.getDescription())
                .transactionDate(request.getTransactionDate())
                .amountUsd(request.getAmountUsd().setScale(2, RoundingMode.HALF_UP))
                .build();

        return repository.save(purchase);

    }

    @Transactional(readOnly = true)
    public TransactionResult getTransactionData(UUID id, String targetCurrency) {
        log.info("Retrieving purchase transaction with key: {}", id);
        return repository.findById(id)
                .map(purchase -> {
                    BigDecimal exchangeRate = exchangeRatesProvider.getExchangeRate(
                            targetCurrency,
                            purchase.getTransactionDate()
                    );
                    log.info("Exchange Rate retrieved for transaction with key: {} is {}", id, exchangeRate);
                    return new TransactionResult(purchase, exchangeRate);
                })
                .orElseThrow(() -> {
                    log.error("Purchase transaction not found for key: {}", id);
                    return new TransactionNotFoundException("Transaction not found with id: " + id);
                });

    }
}