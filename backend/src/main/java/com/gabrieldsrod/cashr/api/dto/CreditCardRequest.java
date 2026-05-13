package com.gabrieldsrod.cashr.api.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreditCardRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String bank;

    @NotNull
    @Min(1) @Max(31)
    private Integer closingDay;

    @NotNull
    @Min(1) @Max(31)
    private Integer dueDay;

    @NotNull
    @Positive
    private BigDecimal creditLimit;

    @NotNull
    private UUID userId;

    @NotNull
    private UUID accountId;
}
