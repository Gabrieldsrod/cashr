package com.gabrieldsrod.cashr.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class InvoiceResponse {

    private CreditCardResponse creditCard;
    private LocalDate invoiceDate;
    private BigDecimal totalAmount;
    private List<TransactionResponse> transactions;
}
