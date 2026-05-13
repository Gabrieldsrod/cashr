package com.gabrieldsrod.cashr.api.service;

import com.gabrieldsrod.cashr.api.dto.AccountRequest;
import com.gabrieldsrod.cashr.api.dto.AccountResponse;
import com.gabrieldsrod.cashr.api.model.Account;
import com.gabrieldsrod.cashr.api.model.TransactionType;
import com.gabrieldsrod.cashr.api.repository.AccountRepository;
import com.gabrieldsrod.cashr.api.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AccountResponse create(AccountRequest request) {
        Account account = Account.builder()
                .name(request.getName())
                .initialBalance(request.getInitialBalance())
                .type(request.getType())
                .build();

        Account saved = accountRepository.save(account);

        return toResponse(saved);
    }

    public List<AccountResponse> findAll() {
        return accountRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public AccountResponse findById(UUID id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        return toResponse(account);
    }

    public void delete(UUID id) {
        accountRepository.deleteById(id);
    }

    private BigDecimal calculateCurrentBalance(UUID accountId, BigDecimal initialBalance) {
        BigDecimal income = transactionRepository.sumAmountByAccountIdAndType(accountId, TransactionType.INCOME);
        BigDecimal expenses = transactionRepository.sumAmountByAccountIdAndType(accountId, TransactionType.EXPENSE);

        return initialBalance.add(income).subtract(expenses);
    }

    private AccountResponse toResponse(Account account) {
        BigDecimal currentBalance = account.getId() != null
                ? calculateCurrentBalance(account.getId(), account.getInitialBalance())
                : account.getInitialBalance();

        return AccountResponse.builder()
                .id(account.getId())
                .name(account.getName())
                .initialBalance(account.getInitialBalance())
                .currentBalance(currentBalance)
                .type(account.getType())
                .build();
    }
}
