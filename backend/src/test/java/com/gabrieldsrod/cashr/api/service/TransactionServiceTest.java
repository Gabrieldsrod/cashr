package com.gabrieldsrod.cashr.api.service;

import com.gabrieldsrod.cashr.api.model.Transaction;
import com.gabrieldsrod.cashr.api.model.TransactionType;
import com.gabrieldsrod.cashr.api.repository.CategoryRepository;
import com.gabrieldsrod.cashr.api.repository.CreditCardRepository;
import com.gabrieldsrod.cashr.api.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CreditCardRepository creditCardRepository;

    @Mock
    private TagService tagService;

    @InjectMocks
    private TransactionService transactionService;

    private final UUID USER_A = UUID.randomUUID();
    private final UUID USER_B = UUID.randomUUID();

    private Transaction income(BigDecimal amount) {
        return Transaction.builder().type(TransactionType.INCOME).amount(amount).build();
    }

    private Transaction expense(BigDecimal amount) {
        return Transaction.builder().type(TransactionType.EXPENSE).amount(amount).build();
    }

    // ── getMonthlyBalance ─────────────────────────────────────────────────────

    @Test
    void getMonthlyBalance_shouldReturnIncomeMinusExpenses() {
        when(transactionRepository.findByUserIdAndCompetenceDateBetween(eq(USER_A), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(
                        income(new BigDecimal("3000.00")),
                        expense(new BigDecimal("800.00")),
                        expense(new BigDecimal("200.00"))
                ));

        BigDecimal balance = transactionService.getMonthlyBalance(USER_A, 2026, 5);

        assertEquals(new BigDecimal("2000.00"), balance);
    }

    // ── isolamento User A vs User B ───────────────────────────────────────────

    @Test
    void getMonthlyBalance_userB_doesNotSeeUserA_transactions() {
        when(transactionRepository.findByUserIdAndCompetenceDateBetween(eq(USER_B), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        BigDecimal balance = transactionService.getMonthlyBalance(USER_B, 2026, 5);

        assertEquals(BigDecimal.ZERO, balance);
        verify(transactionRepository, never()).findByUserIdAndCompetenceDateBetween(eq(USER_A), any(), any());
    }
}
