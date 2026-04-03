package com.sk.ledger.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.ledger.entity.PurchaseTransaction;
import com.sk.ledger.exception.CurrencyRateNotFoundException;
import com.sk.ledger.model.TransactionRequest;
import com.sk.ledger.repository.TransactionRepository;
import com.sk.ledger.service.impl.ExchangeRatesServiceImpl;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PurchaseTransactionsSteps {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TransactionRepository repository;

    @After
    public void tearDown() {
        repository.deleteAll();
    }

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ExchangeRatesServiceImpl exchangeRatesClient;

    private TransactionRequest storeRequest;
    private PurchaseTransaction savedTx;
    private ResultActions response;
    private UUID idempotencyKey;

    // --- GIVEN STEPS ---

    @Given("a purchase exists with an amount of {double} {string} dated {string}")
    public void a_purchase_exists(Double amount, String currency, String datetime) {
        // persist a real record in H2
        this.savedTx = repository.save(PurchaseTransaction.builder()
                .description("ATDD Conversion Test")
                .amountUsd(BigDecimal.valueOf(amount))
                .idempotencyKey(UUID.randomUUID())
                .transactionDate(OffsetDateTime.parse(datetime))
                .build());
    }

    @Given("the Treasury API is stubbed to return {double} for {string}")
    public void stub_treasury_rate(Double rate, String currency) {
        given(exchangeRatesClient.getExchangeRate(eq(currency), any(OffsetDateTime.class)))
                .willReturn(BigDecimal.valueOf(rate));
    }

    @Given("no purchase transaction exists for the provided ID")
    public void noPurchaseTransactionExists() {
        //do nothing
    }

    @Given("the Treasury API has no rate for {string} within the last 6 months")
    public void stub_no_rate(String currency) {

        doThrow(new CurrencyRateNotFoundException("The purchase cannot be converted to the target currency."))
                .when(exchangeRatesClient)
                .getExchangeRate(eq(currency), any());
    }

    @Given("a new transaction request with description {string}, amount {double}, and datetime {string}")
    public void setup_request(String desc, Double amount, String datetime) {
        BigDecimal convertedAmount = new BigDecimal(String.valueOf(amount));
        OffsetDateTime parsedDate = OffsetDateTime.parse(datetime);

        this.storeRequest = TransactionRequest.builder()
                .description(desc)
                .amountUsd(convertedAmount)
                .transactionDate(parsedDate)
                .build();
    }

    @Given("an idempotency key {string}")
    public void set_key(String key) {
        this.idempotencyKey = UUID.fromString(key);
    }

    // --- WHEN STEPS ---

    @When("I request the conversion to {string} for that transaction")
    public void i_request_conversion_with_id(String targetCurrency) throws Exception {
        this.response = mockMvc.perform(get("/api/v1/transactions/" + savedTx.getId() + "/convert")
                .param("targetCurrency", targetCurrency));
    }

    @When("I request the conversion to {string}")
    public void i_request_conversion_generic(String targetCurrency) throws Exception {
        UUID id = (savedTx != null) ? savedTx.getId() : UUID.randomUUID();
        this.response = mockMvc.perform(get("/api/v1/transactions/" + id + "/convert")
                .param("targetCurrency", targetCurrency));
    }

    @When("I submit the store transaction request")
    public void submit_request() throws Exception {
        this.response = mockMvc.perform(post("/api/v1/transactions")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(storeRequest)));
    }

    @When("I request the conversion to {string} for an unknown transaction")
    public void iRequestConversion(String targetCurrency) throws Exception {
        this.response = mockMvc.perform(get("/api/v1/transactions/" + UUID.randomUUID() + "/convert")
                .param("targetCurrency", targetCurrency));
    }

    // --- THEN STEPS ---

    @Then("the response status should be {int}")
    public void verify_status(int statusCode) throws Exception {
        response.andExpect(status().is(statusCode));
    }

    @Then("the database should contain a transaction with description {string}")
    public void verify_db_contains(String expectedDesc) {
        boolean exists = repository.findAll().stream()
                .anyMatch(t -> t.getDescription().equals(expectedDesc));
        assertTrue(exists, "The transaction was not saved to the H2 database.");
    }

    @Then("the stored amount should be {double}")
    public void verify_db_amount(Double expectedAmount) {
        BigDecimal expected = BigDecimal.valueOf(expectedAmount).setScale(2);
        boolean amountMatches = repository.findAll().stream()
                .anyMatch(t -> t.getAmountUsd().setScale(2).equals(expected));
        assertTrue(amountMatches, "The amount in the database does not match the request.");
    }

    @Then("the converted amount should be {double}")
    public void verify_amount(Double expected) throws Exception {
        // JSON Path matches the field name in your TransactionResponse DTO
        response.andExpect(jsonPath("$.convertedAmount").value(expected));
    }

    @Then("the system should return an error {string}")
    public void verify_error_message(String expectedMessage) throws Exception {
        System.out.println("response: " + response);
        response
        .andExpect(jsonPath("$.message").value(containsString(expectedMessage)));
    }

}
