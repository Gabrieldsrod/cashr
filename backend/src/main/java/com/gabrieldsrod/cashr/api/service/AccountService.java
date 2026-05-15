package com.gabrieldsrod.cashr.api.service;

import com.gabrieldsrod.cashr.api.dto.request.AccountRequest;
import com.gabrieldsrod.cashr.api.dto.request.StatementLineResponse;
import com.gabrieldsrod.cashr.api.dto.response.AccountResponse;
import com.gabrieldsrod.cashr.api.exception.BusinessException;
import com.gabrieldsrod.cashr.api.exception.ResourceNotFoundException;
import com.gabrieldsrod.cashr.api.model.Account;
import com.gabrieldsrod.cashr.api.model.Transaction;
import com.gabrieldsrod.cashr.api.model.TransactionStatus;
import com.gabrieldsrod.cashr.api.model.TransactionType;
import com.gabrieldsrod.cashr.api.repository.AccountRepository;
import com.gabrieldsrod.cashr.api.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public List<AccountResponse> findAllByUserId(UUID userId) {
        return accountRepository.findAllByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse findByIdAndUserId(UUID id, UUID userId) {
        return findByIdAndUserId(id, userId, null, null);
    }

    @Transactional(readOnly = true)
    public AccountResponse findByIdAndUserId(UUID id, UUID userId, LocalDate start, LocalDate end) {
        Account account = accountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada"));
        return toResponse(account, start, end);
    }

    @Transactional
    public AccountResponse create(AccountRequest request, UUID userId) {
        if (accountRepository.existsByNameAndUserId(request.getName(), userId)) {
            throw new BusinessException("Account with name '" + request.getName() + "' already exists");
        }

        Account account = Account.builder()
                .name(request.getName())
                .initialBalance(request.getInitialBalance())
                .userId(userId)
                .type(request.getType())
                .currency(request.getCurrency())
                .build();

        return toResponse(accountRepository.save(account));
    }

    @Transactional
    public AccountResponse updateByIdAndUserId(UUID id, AccountRequest request, UUID userId) {
        Account account = accountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada"));

        account.setName(request.getName());
        account.setType(request.getType());

        return toResponse(accountRepository.save(account));
    }

    @Transactional
    public void deleteByIdAndUserId(UUID id, UUID userId) {
        accountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada"));

        if (transactionRepository.existsByAccountId(id)) {
            throw new BusinessException("Não é possível deletar uma conta com transações existentes");
        }

        accountRepository.deleteById(id);
    }

    public List<StatementLineResponse> getStatement(UUID accountId, UUID userId, LocalDate startDate, LocalDate endDate, TransactionStatus status) {
        Account account = accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada"));

        BigDecimal openingIncome = transactionRepository.sumAmountByAccountIdAndTypeAndStatusBefore(accountId, TransactionType.INCOME, TransactionStatus.PAID, startDate);
        BigDecimal openingExpenses = transactionRepository.sumAmountByAccountIdAndTypeAndStatusBefore(accountId, TransactionType.EXPENSE, TransactionStatus.PAID, startDate);
        BigDecimal openingBalance = account.getInitialBalance().add(openingIncome).subtract(openingExpenses);

        List<Transaction> transactions = transactionRepository.findStatement(accountId, startDate, endDate, status);

        AtomicReference<BigDecimal> running = new AtomicReference<>(openingBalance);
        return transactions.stream().map(t -> {
            BigDecimal next = TransactionType.INCOME.equals(t.getType())
                    ? running.get().add(t.getAmount())
                    : running.get().subtract(t.getAmount());
            running.set(next);
            return StatementLineResponse.builder()
                    .id(t.getId())
                    .type(t.getType())
                    .amount(t.getAmount())
                    .competenceDate(t.getCompetenceDate())
                    .description(t.getDescription())
                    .status(t.getStatus())
                    .categoryId(t.getCategory() != null ? t.getCategory().getId() : null)
                    .categoryName(t.getCategory() != null ? t.getCategory().getName() : null)
                    .runningBalance(next)
                    .build();
        }).toList();
    }

    private BigDecimal calculateCurrentBalance(UUID accountId, BigDecimal initialBalance, LocalDate start, LocalDate end) {
        BigDecimal income;
        BigDecimal expenses;

        if (start != null && end != null) {
            income = transactionRepository.sumAmountByAccountIdAndTypeAndStatusAndPeriod(accountId, TransactionType.INCOME, TransactionStatus.PAID, start, end);
            expenses = transactionRepository.sumAmountByAccountIdAndTypeAndStatusAndPeriod(accountId, TransactionType.EXPENSE, TransactionStatus.PAID, start, end);
        } else {
            income = transactionRepository.sumAmountByAccountIdAndTypeAndStatus(accountId, TransactionType.INCOME, TransactionStatus.PAID);
            expenses = transactionRepository.sumAmountByAccountIdAndTypeAndStatus(accountId, TransactionType.EXPENSE, TransactionStatus.PAID);
        }

        return initialBalance.add(income).subtract(expenses);
    }

    private AccountResponse toResponse(Account account) {
        return toResponse(account, null, null);
    }

    private AccountResponse toResponse(Account account, LocalDate start, LocalDate end) {
        BigDecimal currentBalance = account.getId() != null
                ? calculateCurrentBalance(account.getId(), account.getInitialBalance(), start, end)
                : account.getInitialBalance();

        return AccountResponse.builder()
                .id(account.getId())
                .userId(account.getUserId())
                .name(account.getName())
                .initialBalance(account.getInitialBalance())
                .currentBalance(currentBalance)
                .type(account.getType())
                .currency(account.getCurrency())
                .build();
    }
}
