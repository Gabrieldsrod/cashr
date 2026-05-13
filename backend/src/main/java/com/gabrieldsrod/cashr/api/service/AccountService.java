package com.gabrieldsrod.cashr.api.service;

import com.gabrieldsrod.cashr.api.dto.AccountRequest;
import com.gabrieldsrod.cashr.api.dto.AccountResponse;
import com.gabrieldsrod.cashr.api.exception.BusinessException;
import com.gabrieldsrod.cashr.api.model.Account;
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

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public AccountResponse create(AccountRequest request) {
        if (accountRepository.existsByName(request.getName())) {
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

    public List<AccountResponse> findAll() {
        return accountRepository.findAll().stream()
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
            income = transactionRepository.sumAmountByAccountIdAndTypeAndPeriod(accountId, TransactionType.INCOME, start, end);
            expenses = transactionRepository.sumAmountByAccountIdAndTypeAndPeriod(accountId, TransactionType.EXPENSE, start, end);
        } else {
            income = transactionRepository.sumAmountByAccountIdAndType(accountId, TransactionType.INCOME);
            expenses = transactionRepository.sumAmountByAccountIdAndType(accountId, TransactionType.EXPENSE);
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
