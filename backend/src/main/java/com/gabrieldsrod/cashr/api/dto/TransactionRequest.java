package com.gabrieldsrod.cashr.api.dto;

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
    @Positive
    private BigDecimal amount;

    @NotNull
    private LocalDate competenceDate;

    private String description;

    @NotNull
    private UUID categoryId;
}
