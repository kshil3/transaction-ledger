package com.sk.ledger.rest.controller;

import com.sk.ledger.entity.PurchaseTransaction;
import com.sk.ledger.model.TransactionRequest;
import com.sk.ledger.model.TransactionResponse;
import com.sk.ledger.model.TransactionResult;
import com.sk.ledger.model.mapper.TransactionResponseMapper;
import com.sk.ledger.service.LedgerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@Validated
@RequiredArgsConstructor
@Slf4j
public class TransactionsController {

    private final LedgerService ledgerService;

    private final TransactionResponseMapper transactionResponseMapper;

    @PostMapping
    public ResponseEntity<PurchaseTransaction> storeTransaction(@RequestHeader(value = "Idempotency-Key") UUID idempotencyKey,
                                                                @Valid @RequestBody TransactionRequest request) {
        log.info("Received Store Purchase Transaction request : {}", request);
        PurchaseTransaction savedPurchase = ledgerService.saveTransaction(request, idempotencyKey);
        return new ResponseEntity<>(savedPurchase, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/convert")
    public ResponseEntity<TransactionResponse> getTransaction(
            @PathVariable UUID id,
            @RequestParam String targetCurrency) {
        log.info("Received Get ConvertedPurchase Transaction with Id: {} and targetCurrency : {}", id, targetCurrency);
        TransactionResult result = ledgerService.getTransactionData(id, targetCurrency);
        return ResponseEntity.ok(transactionResponseMapper.toResponse(result, targetCurrency));
    }
}
