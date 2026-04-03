package com.sk.ledger.service;

import com.sk.ledger.entity.PurchaseTransaction;
import com.sk.ledger.model.TransactionRequest;
import com.sk.ledger.model.TransactionResult;

import java.util.UUID;

public interface LedgerService {
    PurchaseTransaction saveTransaction(TransactionRequest request, UUID idempotencyKey);
    TransactionResult getTransactionData(UUID id, String targetCurrency);
}