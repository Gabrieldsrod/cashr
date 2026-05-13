package com.gabrieldsrod.cashr.api.dto.response;

import com.gabrieldsrod.cashr.api.model.BudgetStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class BudgetStatusResponse {

    private UUID budgetId;
    private BigDecimal limitAmount;
    private BigDecimal spent;
    private BigDecimal percentageUsed;
    private BudgetStatus status;
}
