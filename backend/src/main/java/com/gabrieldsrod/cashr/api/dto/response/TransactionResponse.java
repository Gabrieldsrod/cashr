package com.gabrieldsrod.cashr.api.dto.response;

import com.gabrieldsrod.cashr.api.model.Currency;
import com.gabrieldsrod.cashr.api.model.PaymentMethod;
import com.gabrieldsrod.cashr.api.model.TransactionStatus;
import com.gabrieldsrod.cashr.api.model.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class TransactionResponse {

    private UUID id;
    private UUID userId;
    private UUID accountId;
    private String accountName;
    private TransactionType type;
    private TransactionStatus status;
    private Currency currency;
    private BigDecimal amount;
    private LocalDate competenceDate;
    private LocalDateTime createdAt;
    private String description;
    private CategoryResponse category;
    private PaymentMethod paymentMethod;
    private CreditCardResponse creditCard;
    private LocalDate invoiceDate;
    private UUID installmentGroupId;
    private Integer installmentNumber;
    private Integer totalInstallments;
    private Set<TagResponse> tags;
}
