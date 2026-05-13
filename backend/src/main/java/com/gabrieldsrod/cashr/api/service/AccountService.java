package com.gabrieldsrod.cashr.api.service;

import com.gabrieldsrod.cashr.api.dto.AccountRequest;
import com.gabrieldsrod.cashr.api.dto.AccountResponse;
import com.gabrieldsrod.cashr.api.dto.StatementLineResponse;
import com.gabrieldsrod.cashr.api.exception.BusinessException;
import com.gabrieldsrod.cashr.api.model.Account;
import com.gabrieldsrod.cashr.api.model.Transaction;
import com.gabrieldsrod.cashr.api.model.TransactionStatus;
import com.gabrieldsrod.cashr.api.model.TransactionType;
import com.gabrieldsrod.cashr.api.model.User;
import com.gabrieldsrod.cashr.api.repository.AccountRepository;
import com.gabrieldsrod.cashr.api.repository.TransactionRepository;
import com.gabrieldsrod.cashr.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
    private final UserRepository userRepository;

    public AccountResponse create(AccountRequest request) {
        if (accountRepository.existsByNameAndUserId(request.getName(), request.getUserId())) {
            throw new BusinessException("Account with name '" + request.getName() + "' already exists");
        }

        Account account = Account.builder()
                .name(request.getName())
                .initialBalance(request.getInitialBalance())
                .user(findUser(request.getUserId()))
                .type(request.getType())
                .currency(request.getCurrency())
                .build();

        return toResponse(accountRepository.save(account));
    }

    public AccountResponse update(UUID id, AccountRequest request) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        account.setName(request.getName());
        account.setUser(findUser(request.getUserId()));
        account.setType(request.getType());
        account.setCurrency(request.getCurrency());

        return toResponse(accountRepository.save(account));
    }

    public List<AccountResponse> findAll(UUID userId) {
        return accountRepository.findAllByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public AccountResponse findById(UUID id) {
        return findById(id, null, null);
    }

    public AccountResponse findById(UUID id, LocalDate start, LocalDate end) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        return toResponse(account, start, end);
    }

    public List<StatementLineResponse> getStatement(UUID accountId, LocalDate startDate, LocalDate endDate, TransactionStatus status) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

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

    public void delete(UUID id) {
        if (transactionRepository.existsByAccountId(id)) {
            throw new BusinessException("Cannot delete account with existing transactions");
        }

        accountRepository.deleteById(id);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));
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
                .userId(account.getUser().getId())
                .name(account.getName())
                .initialBalance(account.getInitialBalance())
                .currentBalance(currentBalance)
                .type(account.getType())
                .currency(account.getCurrency())
                .build();
    }
}
