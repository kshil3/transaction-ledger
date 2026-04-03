package com.sk.ledger.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ExchangeRateData {
    @JsonProperty("record_date")
    private LocalDate recordDate;

    private String currency;

    @JsonProperty("exchange_rate")
    private BigDecimal exchangeRate;
}
