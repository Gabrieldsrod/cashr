package com.gabrieldsrod.cashr.api.service;

import com.gabrieldsrod.cashr.api.dto.AccountRequest;
import com.gabrieldsrod.cashr.api.dto.AccountResponse;
import com.gabrieldsrod.cashr.api.exception.BusinessException;
import com.gabrieldsrod.cashr.api.model.*;
import com.gabrieldsrod.cashr.api.repository.AccountRepository;
import com.gabrieldsrod.cashr.api.repository.TransactionRepository;
import com.gabrieldsrod.cashr.api.repository.UserRepository;
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

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccountService accountService;

    private final UUID USER_ID = UUID.randomUUID();

    private User buildUser() {
        return User.builder().id(USER_ID).email("user@test.com").build();
    }

    private Account buildAccount(UUID id) {
        return Account.builder()
                .id(id)
                .name("Nubank")
                .initialBalance(new BigDecimal("1000.00"))
                .type(AccountType.CHECKING)
                .currency(Currency.BRL)
                .user(buildUser())
                .build();
    }

    private AccountRequest buildRequest(String name, BigDecimal initialBalance, AccountType type) {
        AccountRequest request = new AccountRequest();
        request.setName(name);
        request.setInitialBalance(initialBalance);
        request.setType(type);
        request.setCurrency(Currency.BRL);
        request.setUserId(USER_ID);
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

        when(accountRepository.existsByName("Nubank")).thenReturn(false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(buildUser()));
        when(accountRepository.save(any(Account.class))).thenReturn(buildAccount(id));
        mockBalanceQueries(id, new BigDecimal("500.00"), new BigDecimal("200.00"));

        AccountResponse response = accountService.create(request);

        assertEquals(id, response.getId());
        assertEquals("Nubank", response.getName());
        assertEquals(new BigDecimal("1000.00"), response.getInitialBalance());
        assertEquals(new BigDecimal("1300.00"), response.getCurrentBalance());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void create_shouldThrowWhenNameAlreadyExists() {
        AccountRequest request = buildRequest("Nubank", new BigDecimal("1000.00"), AccountType.CHECKING);

        when(accountRepository.existsByName("Nubank")).thenReturn(true);

        assertThrows(BusinessException.class, () -> accountService.create(request));
        verify(accountRepository, never()).save(any());
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_shouldChangeOnlyNameAndType() {
        UUID id = UUID.randomUUID();
        Account existing = buildAccount(id);

        AccountRequest request = buildRequest("Itaú", new BigDecimal("9999.00"), AccountType.SAVINGS);

        when(accountRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(buildUser()));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        mockBalanceQueries(id, BigDecimal.ZERO, BigDecimal.ZERO);

        AccountResponse response = accountService.update(id, request);

        assertEquals("Itaú", response.getName());
        assertEquals(AccountType.SAVINGS, response.getType());
        assertEquals(new BigDecimal("1000.00"), response.getInitialBalance());
    }

    @Test
    void update_shouldThrowWhenAccountNotFound() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> accountService.update(id, buildRequest("X", BigDecimal.ZERO, AccountType.CASH)));
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_shouldThrowWhenAccountHasTransactions() {
        UUID id = UUID.randomUUID();
        when(transactionRepository.existsByAccountId(id)).thenReturn(true);

        assertThrows(BusinessException.class, () -> accountService.delete(id));
        verify(accountRepository, never()).deleteById(any());
    }

    @Test
    void delete_shouldCallRepositoryWhenNoTransactions() {
        UUID id = UUID.randomUUID();
        when(transactionRepository.existsByAccountId(id)).thenReturn(false);

        accountService.delete(id);

        verify(accountRepository).deleteById(id);
    }

    // ── findAll / findById ────────────────────────────────────────────────────

    @Test
    void findAll_shouldReturnAllAccountsWithCurrentBalance() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        User user = buildUser();

        when(accountRepository.findAll()).thenReturn(List.of(
                Account.builder().id(id1).name("Nubank").initialBalance(new BigDecimal("500.00")).type(AccountType.CHECKING).currency(Currency.BRL).user(user).build(),
                Account.builder().id(id2).name("Carteira").initialBalance(new BigDecimal("100.00")).type(AccountType.CASH).currency(Currency.BRL).user(user).build()
        ));
        mockBalanceQueries(id1, new BigDecimal("200.00"), new BigDecimal("50.00"));
        mockBalanceQueries(id2, BigDecimal.ZERO, BigDecimal.ZERO);

        List<AccountResponse> result = accountService.findAll();

        assertEquals(2, result.size());
        assertEquals(new BigDecimal("650.00"), result.get(0).getCurrentBalance());
        assertEquals(new BigDecimal("100.00"), result.get(1).getCurrentBalance());
    }

    @Test
    void findById_shouldReturnAccountWithCurrentBalance() {
        UUID id = UUID.randomUUID();

        when(accountRepository.findById(id)).thenReturn(Optional.of(buildAccount(id)));
        mockBalanceQueries(id, new BigDecimal("300.00"), new BigDecimal("100.00"));

        AccountResponse response = accountService.findById(id);

        assertEquals(id, response.getId());
        assertEquals(new BigDecimal("1200.00"), response.getCurrentBalance());
    }

    @Test
    void findById_shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> accountService.findById(id));
    }

    @Test
    void currentBalance_shouldBeInitialBalanceWhenNoTransactions() {
        UUID id = UUID.randomUUID();

        when(accountRepository.findById(id)).thenReturn(Optional.of(buildAccount(id)));
        mockBalanceQueries(id, BigDecimal.ZERO, BigDecimal.ZERO);

        AccountResponse response = accountService.findById(id);

        assertEquals(new BigDecimal("1000.00"), response.getCurrentBalance());
    }
}
