package com.gabrieldsrod.cashr.api.dto;

import com.gabrieldsrod.cashr.api.model.TransactionStatus;
import com.gabrieldsrod.cashr.api.model.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class TransactionResponse {

    private UUID id;
    private TransactionType type;
    private TransactionStatus status;
    private BigDecimal amount;
    private LocalDate competenceDate;
    private LocalDateTime createdAt;
    private String description;
    private CategoryResponse category;
}
