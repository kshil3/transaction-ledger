package com.sk.ledger.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionResponse {

    UUID id;
    String description;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "UTC")
    OffsetDateTime transactionDate;
    BigDecimal originalAmountUsd;
    BigDecimal convertedAmount;
    BigDecimal exchangeRate;
    String targetCurrency;

}
