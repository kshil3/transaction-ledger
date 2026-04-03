package com.sk.ledger.service.impl;

import com.sk.ledger.exception.CurrencyRateNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(ExchangeRatesServiceImpl.class)
class ExchangeRatesProviderImplTest {

    // Helper to avoid repetitive JSON strings
    private final String mockApiResponse = "{\"data\": [{\"exchange_rate\": \"5.15\"}]}";
    @Autowired
    private ExchangeRatesServiceImpl client;
    @Autowired
    private MockRestServiceServer server;

    @Test
    @DisplayName("Should build URI with correct filter and sort parameters")
    void getExchangeRate_Success() {
        // Arrange
        String currency = "BRL";
        OffsetDateTime now = Instant.now().atOffset(ZoneOffset.UTC);
        LocalDate currentDate = now.toLocalDate();
        String txDateStr = currentDate.toString();
        String sixMonthsAgoDateStr = currentDate.minusMonths(6).toString();
        String expectedFilter = String.format("country_currency_desc:eq:%s,record_date:gte:%s,record_date:lte:%s",
                currency, sixMonthsAgoDateStr, txDateStr);

        this.server.expect(requestTo(startsWith("https://api.fiscaldata.treasury.gov/services/api/fiscal_service/v1/accounting/od/rates_of_exchange")))
                .andExpect(queryParam("filter", expectedFilter))
                .andExpect(queryParam("sort", "-record_date"))
                .andRespond(withSuccess(mockApiResponse, MediaType.APPLICATION_JSON));

        // Act
        BigDecimal rate = client.getExchangeRate(currency, now);

        // Assert
        assertEquals(new BigDecimal("5.15"), rate);
        this.server.verify();
    }

    @Test
    @DisplayName("Should throw CurrencyRateNotFoundException when API returns empty data list")
    void getExchangeRate_EmptyData_ThrowsException() {
        // Arrange
        this.server.expect(anything())
                .andRespond(withSuccess("{\"data\": []}", MediaType.APPLICATION_JSON));

        // Act & Assert
        CurrencyRateNotFoundException exception = assertThrows(
                CurrencyRateNotFoundException.class,
                () -> client.getExchangeRate("EUR", Instant.now().atOffset(ZoneOffset.UTC))
        );

        assertEquals("The purchase cannot be converted to the target currency.", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw CurrencyRateNotFoundException when API returns null data field")
    void getExchangeRate_NullData_ThrowsException() {
        // Arrange
        this.server.expect(anything())
                .andRespond(withSuccess("{\"data\": null}", MediaType.APPLICATION_JSON));

        // Act & Assert
        assertThrows(CurrencyRateNotFoundException.class,
                () -> client.getExchangeRate("EUR", Instant.now().atOffset(ZoneOffset.UTC)));
    }
}