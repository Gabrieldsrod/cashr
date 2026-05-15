package com.gabrieldsrod.cashr.api.service;

import com.gabrieldsrod.cashr.api.dto.request.InstallmentGroupUpdateRequest;
import com.gabrieldsrod.cashr.api.dto.request.TransactionRequest;
import com.gabrieldsrod.cashr.api.exception.BusinessException;
import com.gabrieldsrod.cashr.api.exception.ResourceNotFoundException;
import com.gabrieldsrod.cashr.api.model.Account;
import com.gabrieldsrod.cashr.api.model.AccountType;
import com.gabrieldsrod.cashr.api.model.Category;
import com.gabrieldsrod.cashr.api.model.Currency;
import com.gabrieldsrod.cashr.api.model.PaymentMethod;
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
        LocalDate start = LocalDate.of(2026, 5, 1);
        LocalDate end = LocalDate.of(2026, 5, 31);
        when(transactionRepository.sumAmountByUserIdAndTypeAndStatusAndPeriod(
                USER_A, TransactionType.INCOME, TransactionStatus.PAID, start, end))
                .thenReturn(new BigDecimal("3000.00"));
        when(transactionRepository.sumAmountByUserIdAndTypeAndStatusAndPeriod(
                USER_A, TransactionType.EXPENSE, TransactionStatus.PAID, start, end))
                .thenReturn(new BigDecimal("1000.00"));

        BigDecimal balance = transactionService.getMonthlyBalance(USER_A, 2026, 5);

        assertEquals(new BigDecimal("2000.00"), balance);
    }

    @Test
    void getMonthlyBalance_queriesOnlyPaidStatus() {
        when(transactionRepository.sumAmountByUserIdAndTypeAndStatusAndPeriod(
                any(UUID.class), any(TransactionType.class), any(TransactionStatus.class),
                any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(BigDecimal.ZERO);

        transactionService.getMonthlyBalance(USER_A, 2026, 5);

        verify(transactionRepository, never()).sumAmountByUserIdAndTypeAndStatusAndPeriod(
                any(), any(), eq(TransactionStatus.PENDING), any(), any());
    }

    // ── isolamento User A vs User B ───────────────────────────────────────────

    @Test
    void getMonthlyBalance_userB_doesNotQueryUserA_data() {
        when(transactionRepository.sumAmountByUserIdAndTypeAndStatusAndPeriod(
                eq(USER_B), any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        BigDecimal balance = transactionService.getMonthlyBalance(USER_B, 2026, 5);

        assertEquals(BigDecimal.ZERO, balance);
        verify(transactionRepository, never()).sumAmountByUserIdAndTypeAndStatusAndPeriod(
                eq(USER_A), any(), any(), any(), any());
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

    // ── update / delete: isolamento por usuário ───────────────────────────────

    @Test
    void update_throwsWhenTransactionBelongsToAnotherUser() {
        UUID transactionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        when(transactionRepository.findByIdAndUserId(transactionId, USER_B)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.update(transactionId, buildRequest(accountId), USER_B));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void update_persistsChangesOnExistingTransaction() {
        UUID transactionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Account account = buildAccount(accountId, USER_A);
        Transaction existing = Transaction.builder()
                .id(transactionId)
                .userId(USER_A)
                .type(TransactionType.EXPENSE)
                .status(TransactionStatus.PENDING)
                .amount(new BigDecimal("50.00"))
                .competenceDate(LocalDate.of(2026, 4, 1))
                .currency(Currency.BRL)
                .build();
        when(transactionRepository.findByIdAndUserId(transactionId, USER_A)).thenReturn(Optional.of(existing));
        when(accountRepository.findByIdAndUserId(accountId, USER_A)).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        transactionService.update(transactionId, buildRequest(accountId), USER_A);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        Transaction saved = captor.getValue();
        assertEquals(new BigDecimal("100.00"), saved.getAmount());
        assertEquals(TransactionStatus.PAID, saved.getStatus());
        assertSame(account, saved.getAccount());
    }

    @Test
    void delete_throwsWhenTransactionBelongsToAnotherUser() {
        UUID transactionId = UUID.randomUUID();
        when(transactionRepository.findByIdAndUserId(transactionId, USER_B)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.delete(transactionId, USER_B));
        verify(transactionRepository, never()).delete(any());
    }

    @Test
    void delete_removesTransactionWhenOwned() {
        UUID transactionId = UUID.randomUUID();
        Transaction existing = Transaction.builder().id(transactionId).userId(USER_A).build();
        when(transactionRepository.findByIdAndUserId(transactionId, USER_A)).thenReturn(Optional.of(existing));

        transactionService.delete(transactionId, USER_A);

        verify(transactionRepository).delete(existing);
    }

    // ── update: paymentMethod imutável ────────────────────────────────────────

    @Test
    void update_throwsWhenPaymentMethodChanges() {
        UUID transactionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Transaction existing = Transaction.builder()
                .id(transactionId)
                .userId(USER_A)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();
        when(transactionRepository.findByIdAndUserId(transactionId, USER_A)).thenReturn(Optional.of(existing));

        TransactionRequest request = buildRequest(accountId);
        request.setPaymentMethod(PaymentMethod.PIX);

        assertThrows(BusinessException.class,
                () -> transactionService.update(transactionId, request, USER_A));
        verify(transactionRepository, never()).save(any());
    }

    // ── updateInstallmentGroup ────────────────────────────────────────────────

    private InstallmentGroupUpdateRequest buildGroupRequest(UUID accountId, UUID categoryId, PaymentMethod paymentMethod) {
        InstallmentGroupUpdateRequest request = new InstallmentGroupUpdateRequest();
        request.setType(TransactionType.EXPENSE);
        request.setStatus(TransactionStatus.PAID);
        request.setCurrency(Currency.BRL);
        request.setAmount(new BigDecimal("100.00"));
        request.setAccountId(accountId);
        request.setCategoryId(categoryId);
        request.setPaymentMethod(paymentMethod);
        return request;
    }

    @Test
    void updateInstallmentGroup_throwsWhenGroupBelongsToAnotherUser() {
        UUID groupId = UUID.randomUUID();
        when(transactionRepository.findByInstallmentGroupIdAndUserIdOrderByInstallmentNumberAsc(groupId, USER_B))
                .thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.updateInstallmentGroup(groupId, buildGroupRequest(UUID.randomUUID(), UUID.randomUUID(), null), USER_B));
        verify(transactionRepository, never()).saveAll(any());
    }

    @Test
    void updateInstallmentGroup_throwsWhenPaymentMethodChanges() {
        UUID groupId = UUID.randomUUID();
        Transaction parcel = Transaction.builder()
                .userId(USER_A)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();
        when(transactionRepository.findByInstallmentGroupIdAndUserIdOrderByInstallmentNumberAsc(groupId, USER_A))
                .thenReturn(List.of(parcel));

        InstallmentGroupUpdateRequest request = buildGroupRequest(UUID.randomUUID(), UUID.randomUUID(), PaymentMethod.PIX);

        assertThrows(BusinessException.class,
                () -> transactionService.updateInstallmentGroup(groupId, request, USER_A));
        verify(transactionRepository, never()).saveAll(any());
    }

    @Test
    void updateInstallmentGroup_appliesChangesToAllParcels() {
        UUID groupId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Account account = buildAccount(accountId, USER_A);
        Category category = Category.builder().id(categoryId).build();

        Transaction p1 = Transaction.builder().userId(USER_A).amount(new BigDecimal("33.33"))
                .competenceDate(LocalDate.of(2026, 5, 1)).installmentNumber(1).build();
        Transaction p2 = Transaction.builder().userId(USER_A).amount(new BigDecimal("33.33"))
                .competenceDate(LocalDate.of(2026, 6, 1)).installmentNumber(2).build();

        when(transactionRepository.findByInstallmentGroupIdAndUserIdOrderByInstallmentNumberAsc(groupId, USER_A))
                .thenReturn(List.of(p1, p2));
        when(accountRepository.findByIdAndUserId(accountId, USER_A)).thenReturn(Optional.of(account));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(transactionRepository.saveAll(any(List.class))).thenAnswer(inv -> inv.getArgument(0));

        transactionService.updateInstallmentGroup(groupId, buildGroupRequest(accountId, categoryId, null), USER_A);

        assertEquals(new BigDecimal("100.00"), p1.getAmount());
        assertEquals(new BigDecimal("100.00"), p2.getAmount());
        assertSame(account, p1.getAccount());
        assertSame(account, p2.getAccount());
        assertEquals(LocalDate.of(2026, 5, 1), p1.getCompetenceDate());
        assertEquals(LocalDate.of(2026, 6, 1), p2.getCompetenceDate());
    }
}
