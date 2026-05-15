package com.gabrieldsrod.cashr.api.dto.request;

import com.gabrieldsrod.cashr.api.model.Currency;
import com.gabrieldsrod.cashr.api.model.PaymentMethod;
import com.gabrieldsrod.cashr.api.model.TransactionStatus;
import com.gabrieldsrod.cashr.api.model.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class TransactionRequest {

    @NotNull
    private TransactionType type;

    @NotNull
    private TransactionStatus status;

    @NotNull
    private Currency currency;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private LocalDate competenceDate;

    private String description;

    @NotNull
    private UUID accountId;

    @NotNull
    private UUID categoryId;

    private PaymentMethod paymentMethod;

    private UUID creditCardId;
}
