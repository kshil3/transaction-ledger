package com.sk.ledger.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.ledger.entity.PurchaseTransaction;
import com.sk.ledger.exception.CurrencyRateNotFoundException;
import com.sk.ledger.exception.TransactionNotFoundException;
import com.sk.ledger.model.TransactionRequest;
import com.sk.ledger.model.TransactionResponse;
import com.sk.ledger.model.TransactionResult;
import com.sk.ledger.model.mapper.TransactionResponseMapper;
import com.sk.ledger.service.impl.LedgerServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionsController.class)
class TransactionsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LedgerServiceImpl ledgerService;

    @MockitoBean
    private TransactionResponseMapper mapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /transactions - Success")
    void storeTransaction_Success() throws Exception {
        // Arrange
        TransactionRequest request = new TransactionRequest();
        request.setDescription("Test Purchase");
        request.setAmountUsd(new BigDecimal("10.50"));
        request.setTransactionDate(Instant.now().atOffset(ZoneOffset.UTC));

        PurchaseTransaction savedEntity = new PurchaseTransaction();
        UUID id = randomUUID();
        savedEntity.setId(id);
        savedEntity.setDescription("Test Purchase");

        when(ledgerService.saveTransaction(any(TransactionRequest.class), any(UUID.class)))
                .thenReturn(savedEntity);

        // Act & Assert
        mockMvc.perform(post("/api/v1/transactions")
                        .header("Idempotency-Key", randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(equalToIgnoringCase(id.toString())))
                .andExpect(jsonPath("$.description").value("Test Purchase"));
    }

    @Test
    @DisplayName("POST /transactions - Failure: Missing Idempotency Key")
    void storeTransaction_MissingHeader_Returns400() throws Exception {
        TransactionRequest request = new TransactionRequest();

        // Act & Assert
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // Spring requires @RequestHeader by default
    }

    @Test
    void getTransaction_Success_Returns200WithData() throws Exception {
        // Arrange
        TransactionResult mockResult = new TransactionResult(null, new BigDecimal("1.23"));
        UUID id = randomUUID();
        TransactionResponse mockResponse = TransactionResponse.builder()
                .id(id)
                .description("Quarterly Subscription")
                .transactionDate(Instant.now().atOffset(ZoneOffset.UTC))
                .originalAmountUsd(new BigDecimal("100.00"))
                .convertedAmount(new BigDecimal("93.50"))
                .exchangeRate(new BigDecimal("0.935"))
                .targetCurrency("EUR")
                .build();

        when(ledgerService.getTransactionData(id, "USD")).thenReturn(mockResult);
        when(mapper.toResponse(mockResult, "USD")).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/transactions/" + id +"/convert")
                        .param("targetCurrency", "USD"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void getTransaction_NotFoundInDB_Returns404() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        when(ledgerService.getTransactionData(eq(id), anyString()))
                .thenThrow(new TransactionNotFoundException("Transaction not found with id: " + id));

        // Act & Assert
        mockMvc.perform(get("/api/v1/transactions/" + id + "/convert")
                        .param("targetCurrency", "USD"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTransaction_RateNotFound_ThrowsException() throws Exception {
        // Arrange: Service throws CurrencyRateNotFoundException
        UUID id = randomUUID();
        when(ledgerService.getTransactionData(any(UUID.class), anyString()))
                .thenThrow(new CurrencyRateNotFoundException("Rate not found"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/transactions/"+id+"/convert")
                        .param("targetCurrency", "INVALID"))
                .andExpect(status().isNotFound());
    }

}
