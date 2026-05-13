package com.gabrieldsrod.cashr.api.service;

import com.gabrieldsrod.cashr.api.model.Transaction;
import com.gabrieldsrod.cashr.api.model.TransactionType;
import com.gabrieldsrod.cashr.api.repository.CategoryRepository;
import com.gabrieldsrod.cashr.api.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void getMonthlyBalance_shouldReturnIncomeMinusExpenses() {
        List<Transaction> transactions = List.of(
                Transaction.builder().type(TransactionType.INCOME).amount(new BigDecimal("3000.00")).competenceDate(LocalDate.of(2026, 5, 1)).build(),
                Transaction.builder().type(TransactionType.EXPENSE).amount(new BigDecimal("800.00")).competenceDate(LocalDate.of(2026, 5, 10)).build(),
                Transaction.builder().type(TransactionType.EXPENSE).amount(new BigDecimal("200.00")).competenceDate(LocalDate.of(2026, 5, 20)).build()
        );

        when(transactionRepository.findByCompetenceDateBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(transactions);

        BigDecimal balance = transactionService.getMonthlyBalance(2026, 5);

        assertEquals(new BigDecimal("2000.00"), balance);
    }
}
