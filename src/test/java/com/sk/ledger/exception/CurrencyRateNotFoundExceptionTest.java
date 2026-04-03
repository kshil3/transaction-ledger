package com.sk.ledger.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrencyRateNotFoundExceptionTest {

    @Test
    void shouldStoreMessage() {
        String errorMessage = "Exchange rate not found for USD";
        CurrencyRateNotFoundException exception = new CurrencyRateNotFoundException(errorMessage);

        assertEquals(errorMessage, exception.getMessage());
        assertTrue(exception instanceof RuntimeException);
    }
}