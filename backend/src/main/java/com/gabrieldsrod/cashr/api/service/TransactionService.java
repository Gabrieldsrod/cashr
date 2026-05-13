package com.gabrieldsrod.cashr.api.service;

import com.gabrieldsrod.cashr.api.dto.CategoryResponse;
import com.gabrieldsrod.cashr.api.dto.CreditCardResponse;
import com.gabrieldsrod.cashr.api.dto.TransactionRequest;
import com.gabrieldsrod.cashr.api.dto.TransactionResponse;
import com.gabrieldsrod.cashr.api.exception.BusinessException;
import com.gabrieldsrod.cashr.api.model.*;
import com.gabrieldsrod.cashr.api.repository.CategoryRepository;
import com.gabrieldsrod.cashr.api.repository.CreditCardRepository;
import com.gabrieldsrod.cashr.api.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final CreditCardRepository creditCardRepository;

    public TransactionResponse create(TransactionRequest request) {
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
        }

        CreditCard creditCard = null;
        LocalDate invoiceDate = null;

        if (PaymentMethod.CREDIT_CARD.equals(request.getPaymentMethod())) {
            if (request.getCreditCardId() == null) {
                throw new BusinessException("creditCardId is required for credit card transactions");
            }
            creditCard = creditCardRepository.findById(request.getCreditCardId())
                    .orElseThrow(() -> new BusinessException("Credit card not found"));
            invoiceDate = calculateInvoiceDate(request.getCompetenceDate(), creditCard);
        }

        Transaction transaction = Transaction.builder()
                .type(request.getType())
                .status(request.getStatus())
                .amount(request.getAmount())
                .competenceDate(request.getCompetenceDate())
                .description(request.getDescription())
                .category(category)
                .paymentMethod(request.getPaymentMethod())
                .creditCard(creditCard)
                .invoiceDate(invoiceDate)
                .build();

        return toResponse(transactionRepository.save(transaction));
    }

    public BigDecimal getMonthlyBalance(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        List<Transaction> transactions = transactionRepository.findByCompetenceDateBetween(start, end);

        BigDecimal income = transactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expenses = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return income.subtract(expenses);
    }

    private LocalDate calculateInvoiceDate(LocalDate purchaseDate, CreditCard card) {
        YearMonth invoiceMonth = purchaseDate.getDayOfMonth() <= card.getClosingDay()
                ? YearMonth.from(purchaseDate)
                : YearMonth.from(purchaseDate).plusMonths(1);

        int lastDay = invoiceMonth.lengthOfMonth();
        int dueDay = Math.min(card.getDueDay(), lastDay);
        return invoiceMonth.atDay(dueDay);
    }

    private TransactionResponse toResponse(Transaction transaction) {
        CategoryResponse categoryResponse = null;
        if (transaction.getCategory() != null) {
            Category cat = transaction.getCategory();
            categoryResponse = CategoryResponse.builder()
                    .id(cat.getId())
                    .name(cat.getName())
                    .description(cat.getDescription())
                    .build();
        }

        CreditCardResponse creditCardResponse = null;
        if (transaction.getCreditCard() != null) {
            CreditCard card = transaction.getCreditCard();
            creditCardResponse = CreditCardResponse.builder()
                    .id(card.getId())
                    .name(card.getName())
                    .bank(card.getBank())
                    .closingDay(card.getClosingDay())
                    .dueDay(card.getDueDay())
                    .creditLimit(card.getCreditLimit())
                    .userId(card.getUser().getId())
                    .build();
        }

        return TransactionResponse.builder()
                .id(transaction.getId())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .competenceDate(transaction.getCompetenceDate())
                .createdAt(transaction.getCreatedAt())
                .description(transaction.getDescription())
                .category(categoryResponse)
                .paymentMethod(transaction.getPaymentMethod())
                .creditCard(creditCardResponse)
                .invoiceDate(transaction.getInvoiceDate())
                .build();
    }
}
