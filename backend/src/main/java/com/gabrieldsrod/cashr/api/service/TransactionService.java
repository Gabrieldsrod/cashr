package com.gabrieldsrod.cashr.api.service;

import com.gabrieldsrod.cashr.api.dto.*;

import com.gabrieldsrod.cashr.api.exception.BusinessException;
import com.gabrieldsrod.cashr.api.model.*;
import com.gabrieldsrod.cashr.api.repository.CategoryRepository;
import com.gabrieldsrod.cashr.api.repository.CreditCardRepository;
import com.gabrieldsrod.cashr.api.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    public TransactionResponse findById(UUID id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Transaction not found"));
        return toResponse(transaction);
    }

    public List<TransactionResponse> findAll(TransactionType type, TransactionStatus status, Integer year, Integer month) {
        LocalDate start = null;
        LocalDate end = null;
        if (year != null && month != null) {
            YearMonth ym = YearMonth.of(year, month);
            start = ym.atDay(1);
            end = ym.atEndOfMonth();
        }
        return transactionRepository.findWithFilters(type, status, start, end)
                .stream().map(this::toResponse).toList();
    }

    public List<TransactionResponse> findByInstallmentGroup(UUID groupId) {
        return transactionRepository.findByInstallmentGroupIdOrderByInstallmentNumberAsc(groupId)
                .stream().map(this::toResponse).toList();
    }

    public List<TransactionResponse> createInstallments(InstallmentRequest request) {
        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
        }

        CreditCard creditCard = null;
        if (PaymentMethod.CREDIT_CARD.equals(request.getPaymentMethod())) {
            if (request.getCreditCardId() == null) {
                throw new BusinessException("creditCardId is required for credit card transactions");
            }
            creditCard = creditCardRepository.findById(request.getCreditCardId())
                    .orElseThrow(() -> new BusinessException("Credit card not found"));
        }

        int total = request.getTotalInstallments();
        BigDecimal installmentAmount = request.getAmount()
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        UUID groupId = UUID.randomUUID();
        List<Transaction> transactions = new ArrayList<>();

        for (int i = 1; i <= total; i++) {
            LocalDate competenceDate = request.getCompetenceDate().plusMonths(i - 1);
            LocalDate invoiceDate = null;
            if (creditCard != null) {
                invoiceDate = calculateInvoiceDate(competenceDate, creditCard);
            }

            transactions.add(Transaction.builder()
                    .type(request.getType())
                    .status(request.getStatus())
                    .amount(installmentAmount)
                    .competenceDate(competenceDate)
                    .description(request.getDescription())
                    .category(category)
                    .paymentMethod(request.getPaymentMethod())
                    .creditCard(creditCard)
                    .invoiceDate(invoiceDate)
                    .installmentGroupId(groupId)
                    .installmentNumber(i)
                    .totalInstallments(total)
                    .build());
        }

        return transactionRepository.saveAll(transactions).stream()
                .map(this::toResponse)
                .toList();
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
                .installmentGroupId(transaction.getInstallmentGroupId())
                .installmentNumber(transaction.getInstallmentNumber())
                .totalInstallments(transaction.getTotalInstallments())
                .build();
    }
}
