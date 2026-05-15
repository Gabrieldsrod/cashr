package com.gabrieldsrod.cashr.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class BudgetResponse {

    private UUID id;
    private UUID categoryId;
    private String categoryName;
    private LocalDate month;
    private BigDecimal limitAmount;
}
