package com.gabrieldsrod.cashr.api.dto;

import com.gabrieldsrod.cashr.api.model.AccountType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class AccountResponse {

    private UUID id;
    private String name;
    private BigDecimal initialBalance;
    private BigDecimal currentBalance;
    private AccountType type;
}
