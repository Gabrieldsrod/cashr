package com.gabrieldsrod.cashr.api.service;

import com.gabrieldsrod.cashr.api.dto.TransactionRequest;
import com.gabrieldsrod.cashr.api.dto.TransactionResponse;
import com.gabrieldsrod.cashr.api.model.Transaction;
import com.gabrieldsrod.cashr.api.model.TransactionType;
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

    public TransactionResponse create(TransactionRequest request) {
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        Transaction transaction = Transaction.builder()
                .type(request.getType())
                .amount(request.getAmount())
                .date(request.getDate())
                .description(request.getDescription())
                .build();

        Transaction saved = transactionRepository.save(transaction);

        return TransactionResponse.builder()
                .id(saved.getId())
                .type(saved.getType())
                .amount(saved.getAmount())
                .date(saved.getDate())
                .description(saved.getDescription())
                .build();
    }

    public BigDecimal getMonthlyBalance(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        List<Transaction> transactions = transactionRepository.findByDateBetween(start, end);

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
}
