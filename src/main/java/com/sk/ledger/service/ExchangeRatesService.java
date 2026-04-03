package com.sk.ledger.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public interface ExchangeRatesService {
    BigDecimal getExchangeRate(String targetCurrency, OffsetDateTime transactionDate);
}
