package com.sk.ledger.service.impl;

import com.sk.ledger.exception.CurrencyRateNotFoundException;
import com.sk.ledger.model.ExchangeRatesResponse;
import com.sk.ledger.service.ExchangeRatesService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Component
@Slf4j
public class ExchangeRatesServiceImpl implements ExchangeRatesService {

    private static final String BASE_URL = "https://api.fiscaldata.treasury.gov/services/api/fiscal_service/v1/accounting/od/rates_of_exchange";
    private final RestClient restClient;

    public ExchangeRatesServiceImpl(RestClient.Builder builder) {
        this.restClient = builder.baseUrl(BASE_URL).build();
    }

    @Retry(name = "exchangeRatesApi") // TODO: No fallbackMethod
    @RateLimiter(name = "exchangeRatesLimit")
    @CircuitBreaker(name = "exchangeRatesCircuit")
    public BigDecimal getExchangeRate(String targetCurrency, OffsetDateTime transactionDate) {
        String txDate = transactionDate.toLocalDate().toString();
        String sixMonthsAgoDate = transactionDate.toLocalDate()
                .minusMonths(6)
                .toString();

        // Filter parameters for the API
        String filter = String.format(
                "country_currency_desc:eq:%s,record_date:gte:%s,record_date:lte:%s", // country_currency_desc must match
                targetCurrency, sixMonthsAgoDate, txDate //record_date must be between (Date - 6 months) and Date
        );

        log.info("Fetching exchange rate for currency: {} on date: {}", targetCurrency, transactionDate);

        ExchangeRatesResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("filter", filter)
                        .queryParam("sort", "-record_date") // Get latest date first
                        .build())
                .retrieve()
                .body(ExchangeRatesResponse.class);

        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            throw new CurrencyRateNotFoundException(
                    "The purchase cannot be converted to the target currency."
            );
        }

        // Return the most recent rate (first in sorted list)
        return response.getData().get(0).getExchangeRate();
    }
}