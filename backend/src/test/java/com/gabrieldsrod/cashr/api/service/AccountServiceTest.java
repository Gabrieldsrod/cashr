package com.gabrieldsrod.cashr.api.service;

import com.gabrieldsrod.cashr.api.dto.request.AccountRequest;
import com.gabrieldsrod.cashr.api.dto.response.AccountResponse;
import com.gabrieldsrod.cashr.api.exception.BusinessException;
import com.gabrieldsrod.cashr.api.model.*;
import com.gabrieldsrod.cashr.api.repository.AccountRepository;
import com.gabrieldsrod.cashr.api.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

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

    private AccountRequest buildRequest(String name, BigDecimal initialBalance, AccountType type) {
        AccountRequest request = new AccountRequest();
        request.setName(name);
        request.setInitialBalance(initialBalance);
        request.setType(type);
        request.setCurrency(Currency.BRL);
        return request;
    }

    private void mockBalanceQueries(UUID id, BigDecimal income, BigDecimal expenses) {
        when(transactionRepository.sumAmountByAccountIdAndTypeAndStatus(id, TransactionType.INCOME, TransactionStatus.PAID)).thenReturn(income);
        when(transactionRepository.sumAmountByAccountIdAndTypeAndStatus(id, TransactionType.EXPENSE, TransactionStatus.PAID)).thenReturn(expenses);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_shouldReturnAccountResponseWithCurrentBalance() {
        UUID id = UUID.randomUUID();
        AccountRequest request = buildRequest("Nubank", new BigDecimal("1000.00"), AccountType.CHECKING);

        when(accountRepository.existsByNameAndUserId("Nubank", USER_A)).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(buildAccount(id, USER_A));
        mockBalanceQueries(id, new BigDecimal("500.00"), new BigDecimal("200.00"));

        AccountResponse response = accountService.create(request, USER_A);

        assertEquals(id, response.getId());
        assertEquals(new BigDecimal("1300.00"), response.getCurrentBalance());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void create_shouldThrowWhenNameAlreadyExists() {
        AccountRequest request = buildRequest("Nubank", new BigDecimal("1000.00"), AccountType.CHECKING);
        when(accountRepository.existsByNameAndUserId("Nubank", USER_A)).thenReturn(true);

        assertThrows(BusinessException.class, () -> accountService.create(request, USER_A));
        verify(accountRepository, never()).save(any());
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_shouldChangeOnlyNameAndType() {
        UUID id = UUID.randomUUID();
        AccountRequest request = buildRequest("Itaú", new BigDecimal("9999.00"), AccountType.SAVINGS);

        when(accountRepository.findByIdAndUserId(id, USER_A)).thenReturn(Optional.of(buildAccount(id, USER_A)));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        mockBalanceQueries(id, BigDecimal.ZERO, BigDecimal.ZERO);

        AccountResponse response = accountService.updateByIdAndUserId(id, request, USER_A);

        assertEquals("Itaú", response.getName());
        assertEquals(AccountType.SAVINGS, response.getType());
        assertEquals(new BigDecimal("1000.00"), response.getInitialBalance());
    }

    @Test
    void update_shouldThrowWhenAccountNotFound() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findByIdAndUserId(id, USER_A)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> accountService.updateByIdAndUserId(id, buildRequest("X", BigDecimal.ZERO, AccountType.CASH), USER_A));
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_shouldThrowWhenAccountHasTransactions() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findByIdAndUserId(id, USER_A)).thenReturn(Optional.of(buildAccount(id, USER_A)));
        when(transactionRepository.existsByAccountId(id)).thenReturn(true);

        assertThrows(BusinessException.class, () -> accountService.deleteByIdAndUserId(id, USER_A));
        verify(accountRepository, never()).deleteById(any());
    }

    @Test
    void delete_shouldCallRepositoryWhenNoTransactions() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findByIdAndUserId(id, USER_A)).thenReturn(Optional.of(buildAccount(id, USER_A)));
        when(transactionRepository.existsByAccountId(id)).thenReturn(false);

        accountService.deleteByIdAndUserId(id, USER_A);

        verify(accountRepository).deleteById(id);
    }

    // ── findAll / findById ────────────────────────────────────────────────────

    @Test
    void findAll_shouldReturnAllAccountsWithCurrentBalance() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        when(accountRepository.findAllByUserId(USER_A)).thenReturn(List.of(
                Account.builder().id(id1).name("Nubank").initialBalance(new BigDecimal("500.00")).type(AccountType.CHECKING).currency(Currency.BRL).userId(USER_A).build(),
                Account.builder().id(id2).name("Carteira").initialBalance(new BigDecimal("100.00")).type(AccountType.CASH).currency(Currency.BRL).userId(USER_A).build()
        ));
        mockBalanceQueries(id1, new BigDecimal("200.00"), new BigDecimal("50.00"));
        mockBalanceQueries(id2, BigDecimal.ZERO, BigDecimal.ZERO);

        List<AccountResponse> result = accountService.findAllByUserId(USER_A);

        assertEquals(2, result.size());
        assertEquals(new BigDecimal("650.00"), result.get(0).getCurrentBalance());
        assertEquals(new BigDecimal("100.00"), result.get(1).getCurrentBalance());
    }

    @Test
    void findById_shouldReturnAccountWithCurrentBalance() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findByIdAndUserId(id, USER_A)).thenReturn(Optional.of(buildAccount(id, USER_A)));
        mockBalanceQueries(id, new BigDecimal("300.00"), new BigDecimal("100.00"));

        AccountResponse response = accountService.findByIdAndUserId(id, USER_A);

        assertEquals(id, response.getId());
        assertEquals(new BigDecimal("1200.00"), response.getCurrentBalance());
    }

    @Test
    void findById_shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findByIdAndUserId(id, USER_A)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> accountService.findByIdAndUserId(id, USER_A));
    }

    // ── isolamento User A vs User B ───────────────────────────────────────────

    @Test
    void findById_userB_cannotReadUserA_account() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findByIdAndUserId(id, USER_B)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> accountService.findByIdAndUserId(id, USER_B));
        verify(accountRepository, never()).findByIdAndUserId(id, USER_A);
    }

    @Test
    void update_userB_cannotUpdateUserA_account() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findByIdAndUserId(id, USER_B)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> accountService.updateByIdAndUserId(id, buildRequest("Hack", BigDecimal.ZERO, AccountType.CASH), USER_B));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void delete_userB_cannotDeleteUserA_account() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findByIdAndUserId(id, USER_B)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> accountService.deleteByIdAndUserId(id, USER_B));
        verify(accountRepository, never()).deleteById(any());
    }

    @Test
    void findAll_userB_doesNotSeeUserA_accounts() {
        when(accountRepository.findAllByUserId(USER_B)).thenReturn(List.of());

        List<AccountResponse> result = accountService.findAllByUserId(USER_B);

        assertTrue(result.isEmpty());
        verify(accountRepository, never()).findAllByUserId(USER_A);
    }
}
