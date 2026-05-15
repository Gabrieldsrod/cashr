package com.gabrieldsrod.cashr.api.service;

import com.gabrieldsrod.cashr.api.dto.request.TransactionRequest;
import com.gabrieldsrod.cashr.api.exception.ResourceNotFoundException;
import com.gabrieldsrod.cashr.api.model.Account;
import com.gabrieldsrod.cashr.api.model.AccountType;
import com.gabrieldsrod.cashr.api.model.Currency;
import com.gabrieldsrod.cashr.api.model.Transaction;
import com.gabrieldsrod.cashr.api.model.TransactionStatus;
import com.gabrieldsrod.cashr.api.model.TransactionType;
import com.gabrieldsrod.cashr.api.repository.AccountRepository;
import com.gabrieldsrod.cashr.api.repository.CategoryRepository;
import com.gabrieldsrod.cashr.api.repository.CreditCardRepository;
import com.gabrieldsrod.cashr.api.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private AccountRepository accountRepository;

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

    private Account buildAccount(UUID id, UUID userId) {
        return Account.builder()
                .id(id)
                .name("Nubank")
                .initialBalance(new BigDecimal("1000.00"))
                .type(AccountType.CHECKING)
                .currency(Currency.BRL)
                .userId(userId)
                .build();
    }

    private TransactionRequest buildRequest(UUID accountId) {
        TransactionRequest request = new TransactionRequest();
        request.setType(TransactionType.EXPENSE);
        request.setStatus(TransactionStatus.PAID);
        request.setCurrency(Currency.BRL);
        request.setAmount(new BigDecimal("100.00"));
        request.setCompetenceDate(LocalDate.of(2026, 5, 14));
        request.setAccountId(accountId);
        request.setCategoryId(null);
        return request;
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

    // ── findById: isolamento por usuário ──────────────────────────────────────

    @Test
    void findById_userB_cannotReadUserA_transaction() {
        UUID transactionId = UUID.randomUUID();
        when(transactionRepository.findByIdAndUserId(transactionId, USER_B)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.findById(transactionId, USER_B));
    }

    // ── findByInstallmentGroup: isolamento por usuário ────────────────────────

    @Test
    void findByInstallmentGroup_userB_seesEmptyList_whenGroupBelongsToUserA() {
        UUID groupId = UUID.randomUUID();
        when(transactionRepository.findByInstallmentGroupIdAndUserIdOrderByInstallmentNumberAsc(groupId, USER_B))
                .thenReturn(List.of());

        assertEquals(0, transactionService.findByInstallmentGroup(groupId, USER_B).size());
    }

    // ── create: associa transação à conta correta ─────────────────────────────

    @Test
    void create_associatesTransactionWithCorrectAccount() {
        UUID accountId = UUID.randomUUID();
        Account account = buildAccount(accountId, USER_A);
        when(accountRepository.findByIdAndUserId(accountId, USER_A)).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        transactionService.create(buildRequest(accountId), USER_A);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertSame(account, captor.getValue().getAccount());
        assertEquals(USER_A, captor.getValue().getUserId());
    }

    @Test
    void create_throwsWhenAccountBelongsToAnotherUser() {
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findByIdAndUserId(accountId, USER_B)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.create(buildRequest(accountId), USER_B));
        verify(transactionRepository, never()).save(any());
    }
}
