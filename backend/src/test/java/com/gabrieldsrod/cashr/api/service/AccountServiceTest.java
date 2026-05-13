package com.gabrieldsrod.cashr.api.service;

import com.gabrieldsrod.cashr.api.dto.AccountRequest;
import com.gabrieldsrod.cashr.api.dto.AccountResponse;
import com.gabrieldsrod.cashr.api.model.Account;
import com.gabrieldsrod.cashr.api.model.AccountType;
import com.gabrieldsrod.cashr.api.model.TransactionType;
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

    private Account buildAccount(UUID id) {
        return Account.builder()
                .id(id)
                .name("Nubank")
                .initialBalance(new BigDecimal("1000.00"))
                .type(AccountType.CHECKING)
                .build();
    }

    private void mockBalanceQueries(UUID id, BigDecimal income, BigDecimal expenses) {
        when(transactionRepository.sumAmountByAccountIdAndType(id, TransactionType.INCOME)).thenReturn(income);
        when(transactionRepository.sumAmountByAccountIdAndType(id, TransactionType.EXPENSE)).thenReturn(expenses);
    }

    @Test
    void create_shouldReturnAccountResponseWithCurrentBalance() {
        UUID id = UUID.randomUUID();
        AccountRequest request = new AccountRequest();
        request.setName("Nubank");
        request.setInitialBalance(new BigDecimal("1000.00"));
        request.setType(AccountType.CHECKING);

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
    void findAll_shouldReturnAllAccountsWithCurrentBalance() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        when(accountRepository.findAll()).thenReturn(List.of(
                Account.builder().id(id1).name("Nubank").initialBalance(new BigDecimal("500.00")).type(AccountType.CHECKING).build(),
                Account.builder().id(id2).name("Carteira").initialBalance(new BigDecimal("100.00")).type(AccountType.CASH).build()
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
    void delete_shouldCallRepositoryDeleteById() {
        UUID id = UUID.randomUUID();

        accountService.delete(id);

        verify(accountRepository).deleteById(id);
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
