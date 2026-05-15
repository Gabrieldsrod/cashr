package com.gabrieldsrod.cashr.api.service;

import com.gabrieldsrod.cashr.api.dto.request.InstallmentGroupUpdateRequest;
import com.gabrieldsrod.cashr.api.dto.request.InstallmentRequest;
import com.gabrieldsrod.cashr.api.dto.request.TransactionRequest;
import com.gabrieldsrod.cashr.api.dto.response.CategoryResponse;
import com.gabrieldsrod.cashr.api.dto.response.CreditCardResponse;
import com.gabrieldsrod.cashr.api.dto.response.TagResponse;
import com.gabrieldsrod.cashr.api.dto.response.TransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.gabrieldsrod.cashr.api.exception.BusinessException;
import com.gabrieldsrod.cashr.api.exception.ResourceNotFoundException;
import com.gabrieldsrod.cashr.api.model.*;

import com.gabrieldsrod.cashr.api.repository.AccountRepository;
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final CreditCardRepository creditCardRepository;
    private final AccountRepository accountRepository;
    private final TagService tagService;

    public TransactionResponse create(TransactionRequest request, UUID userId) {
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        Account account = accountRepository.findByIdAndUserId(request.getAccountId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada"));

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
                .userId(userId)
                .type(request.getType())
                .status(request.getStatus())
                .currency(request.getCurrency())
                .amount(request.getAmount())
                .competenceDate(request.getCompetenceDate())
                .description(request.getDescription())
                .account(account)
                .category(category)
                .paymentMethod(request.getPaymentMethod())
                .creditCard(creditCard)
                .invoiceDate(invoiceDate)
                .build();

        return toResponse(transactionRepository.save(transaction));
    }

    public TransactionResponse findById(UUID id, UUID userId) {
        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada"));
        return toResponse(transaction);
    }

    public TransactionResponse update(UUID id, TransactionRequest request, UUID userId) {
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada"));

        if (transaction.getPaymentMethod() != request.getPaymentMethod()) {
            throw new BusinessException("paymentMethod cannot be changed; delete and recreate the transaction");
        }

        Account account = accountRepository.findByIdAndUserId(request.getAccountId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada"));

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
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

        transaction.setType(request.getType());
        transaction.setStatus(request.getStatus());
        transaction.setCurrency(request.getCurrency());
        transaction.setAmount(request.getAmount());
        transaction.setCompetenceDate(request.getCompetenceDate());
        transaction.setDescription(request.getDescription());
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setPaymentMethod(request.getPaymentMethod());
        transaction.setCreditCard(creditCard);
        transaction.setInvoiceDate(invoiceDate);

        return toResponse(transactionRepository.save(transaction));
    }

    public void delete(UUID id, UUID userId) {
        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada"));
        transactionRepository.delete(transaction);
    }

    public List<TransactionResponse> updateInstallmentGroup(UUID groupId, InstallmentGroupUpdateRequest request, UUID userId) {
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        List<Transaction> transactions = transactionRepository.findByInstallmentGroupIdAndUserIdOrderByInstallmentNumberAsc(groupId, userId);
        if (transactions.isEmpty()) {
            throw new ResourceNotFoundException("Grupo de parcelas não encontrado");
        }

        if (transactions.get(0).getPaymentMethod() != request.getPaymentMethod()) {
            throw new BusinessException("paymentMethod cannot be changed; delete and recreate the installment group");
        }

        Account account = accountRepository.findByIdAndUserId(request.getAccountId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        CreditCard creditCard = null;
        if (PaymentMethod.CREDIT_CARD.equals(request.getPaymentMethod())) {
            if (request.getCreditCardId() == null) {
                throw new BusinessException("creditCardId is required for credit card transactions");
            }
            creditCard = creditCardRepository.findById(request.getCreditCardId())
                    .orElseThrow(() -> new BusinessException("Credit card not found"));
        }

        for (Transaction tx : transactions) {
            tx.setType(request.getType());
            tx.setStatus(request.getStatus());
            tx.setCurrency(request.getCurrency());
            tx.setAmount(request.getAmount());
            tx.setDescription(request.getDescription());
            tx.setAccount(account);
            tx.setCategory(category);
            tx.setCreditCard(creditCard);
            tx.setInvoiceDate(creditCard != null ? calculateInvoiceDate(tx.getCompetenceDate(), creditCard) : null);
        }

        return transactionRepository.saveAll(transactions).stream()
                .map(this::toResponse)
                .toList();
    }

    public Page<TransactionResponse> findAll(UUID userId, TransactionType type, TransactionStatus status, Integer year, Integer month, Pageable pageable) {
        LocalDate start = null;
        LocalDate end = null;
        if (year != null && month != null) {
            YearMonth ym = YearMonth.of(year, month);
            start = ym.atDay(1);
            end = ym.atEndOfMonth();
        }
        return transactionRepository.findWithFilters(userId, type, status, start, end, pageable)
                .map(this::toResponse);
    }

    public List<TransactionResponse> findByInstallmentGroup(UUID groupId, UUID userId) {
        return transactionRepository.findByInstallmentGroupIdAndUserIdOrderByInstallmentNumberAsc(groupId, userId)
                .stream().map(this::toResponse).toList();
    }

    public List<TransactionResponse> createInstallments(InstallmentRequest request, UUID userId) {
        Account account = accountRepository.findByIdAndUserId(request.getAccountId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada"));

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
                    .userId(userId)
                    .type(request.getType())
                    .status(request.getStatus())
                    .currency(request.getCurrency())
                    .amount(installmentAmount)
                    .competenceDate(competenceDate)
                    .description(request.getDescription())
                    .account(account)
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

    public BigDecimal getMonthlyBalance(UUID userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        BigDecimal income = transactionRepository.sumAmountByUserIdAndTypeAndStatusAndPeriod(
                userId, TransactionType.INCOME, TransactionStatus.PAID, start, end);
        BigDecimal expenses = transactionRepository.sumAmountByUserIdAndTypeAndStatusAndPeriod(
                userId, TransactionType.EXPENSE, TransactionStatus.PAID, start, end);

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
                    .userId(cat.getUserId())
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

        Account txAccount = transaction.getAccount();

        return TransactionResponse.builder()
                .id(transaction.getId())
                .userId(transaction.getUserId())
                .accountId(txAccount != null ? txAccount.getId() : null)
                .accountName(txAccount != null ? txAccount.getName() : null)
                .type(transaction.getType())
                .status(transaction.getStatus())
                .currency(transaction.getCurrency())
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
                .tags(transaction.getTags().stream().map(tagService::toResponse).collect(Collectors.toSet()))
                .build();
    }
}
