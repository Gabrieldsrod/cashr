package com.gabrieldsrod.cashr.api.dto.request;

import com.gabrieldsrod.cashr.api.model.TransactionStatus;
import com.gabrieldsrod.cashr.api.model.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class StatementLineResponse {
    private UUID id;
    private TransactionType type;
    private BigDecimal amount;
    private LocalDate competenceDate;
    private String description;
    private TransactionStatus status;
    private UUID categoryId;
    private String categoryName;
    private BigDecimal runningBalance;
}
