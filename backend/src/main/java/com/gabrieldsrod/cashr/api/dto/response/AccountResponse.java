package com.gabrieldsrod.cashr.api.dto.response;

import com.gabrieldsrod.cashr.api.model.AccountType;
import com.gabrieldsrod.cashr.api.model.Currency;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class AccountResponse {

    private UUID id;
    private UUID userId;
    private String name;
    private BigDecimal initialBalance;
    private BigDecimal currentBalance;
    private AccountType type;
    private Currency currency;
}
