package com.gabrieldsrod.cashr.api.dto;

import com.gabrieldsrod.cashr.api.model.AccountType;
import com.gabrieldsrod.cashr.api.model.Currency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountRequest {

    @NotBlank
    private String name;

    @NotNull
    @PositiveOrZero
    private BigDecimal initialBalance;

    @NotNull
    private AccountType type;

    @NotNull
    private Currency currency;
}
