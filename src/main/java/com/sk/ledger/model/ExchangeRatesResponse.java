package com.sk.ledger.model;

import lombok.Data;

import java.util.List;

@Data
public class ExchangeRatesResponse {
    private List<ExchangeRateData> data;
}