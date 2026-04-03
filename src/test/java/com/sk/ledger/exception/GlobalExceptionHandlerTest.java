package com.sk.ledger.exception;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;
    private static final String CONVERSION_MSG = "The purchase cannot be converted to the target currency.";

    @BeforeEach
    void setUp() {
        // StandaloneSetup to test the ExceptionHandler specifically
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturn404_WhenCurrencyNotFound() throws Exception {
        mockMvc.perform(get("/test/currency-error"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is(CONVERSION_MSG)));
    }

    @Test
    void shouldReturn400_WhenValidationFails() throws Exception {
        mockMvc.perform(post("/test/validation-error")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")) // Empty body triggers @NotNull
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Validation Failed")))
                .andExpect(jsonPath("$.errors.name", is("Name required")));
    }

    @Test
    void shouldReturn409_WhenDuplicateExists() throws Exception {
        mockMvc.perform(get("/test/duplicates"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void shouldReturn429_WhenRateLimited() throws Exception {
        mockMvc.perform(get("/test/rate-limit"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message", is("Rate limit exceeded")));
    }

    @Test
    void shouldReturn503_WhenCircuitBreakerOpen() throws Exception {
        mockMvc.perform(get("/test/circuit-breaker"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message", is("Service temporarily unavailable")));
    }

    @Test
    void shouldReturn500_ForGeneralError() throws Exception {
        mockMvc.perform(get("/test/general-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", is("An unexpected error occurred")));
    }

    // Dummy Controller to throw exceptions for testing
    @RestController
    static class TestController {

        @PostMapping("/test/validation-error")
        public void throwValidationErrors(@Valid @RequestBody TestDto dto) {}

        @GetMapping("/test/currency-error")
        public void throwCurrencyNotFound() { throw new CurrencyRateNotFoundException(CONVERSION_MSG); }

        @GetMapping("/test/duplicates")
        public void throwDupe() { throw new DuplicateTransactionException("already exists"); }

        @GetMapping("/test/rate-limit")
        public void throwRate() {
            throw RequestNotPermitted.createRequestNotPermitted(io.github.resilience4j.ratelimiter.RateLimiter.ofDefaults("test"));
        }

        @GetMapping("/test/circuit-breaker")
        public void throwCktBkr() {
            throw CallNotPermittedException.createCallNotPermittedException(CircuitBreaker.ofDefaults("test"));
        }

        @GetMapping("/test/general-error")
        public void throwGeneral() { throw new RuntimeException("Crash"); }
    }

    //Dummy object to test validation errors
    static class TestDto {
        @NotNull(message = "Name required")
        public String name;
    }
}