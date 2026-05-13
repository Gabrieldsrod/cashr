package com.gabrieldsrod.cashr.api.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class CreditCardResponse {

    private UUID id;
    private String name;
    private String bank;
    private Integer closingDay;
    private Integer dueDay;
    private BigDecimal creditLimit;
    private UUID userId;
    private UUID accountId;
}
