package com.sk.ledger.model.mapper;

import com.sk.ledger.model.TransactionResponse;
import com.sk.ledger.model.TransactionResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Service calls this mapper to convert a TransactionResult into a TransactionResponse.
 */
@Mapper(componentModel = "spring")
public interface TransactionResponseMapper {

    default TransactionResponse toResponse(TransactionResult result, String targetCurrency) {
        if (result == null) {
            return null;
        }
        return mapToResponse(result, targetCurrency);
    }

    default BigDecimal calculateConverted(TransactionResult result) {
        if (result == null || result.getTransaction() == null || result.getExchangeRate() == null) {
            return null;
        }

        // Multiply: Amount * Rate
        // Round: 2 decimal places, HALF_UP (0.005 becomes 0.01)
        return result.getTransaction().getAmountUsd()
                .multiply(result.getExchangeRate())
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * MapStruct will implement this internal mapping logic.
     */
    @Mapping(target = "id", source = "result.transaction.id")
    @Mapping(target = "description", source = "result.transaction.description")
    @Mapping(target = "transactionDate", source = "result.transaction.transactionDate")
    @Mapping(target = "originalAmountUsd", source = "result.transaction.amountUsd")
    @Mapping(target = "exchangeRate", ignore = true)
    @Mapping(target = "targetCurrency", source = "targetCurrency")
    @Mapping(target = "convertedAmount", expression = "java(calculateConverted(result))")
    TransactionResponse mapToResponse(TransactionResult result, String targetCurrency);
}

