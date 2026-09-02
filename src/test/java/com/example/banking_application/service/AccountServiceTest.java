package com.example.banking_application.service;

import com.example.banking_application.dto.AccountRequest;
import com.example.banking_application.dto.AccountResponse;
import com.example.banking_application.dto.AccountStatusRequest;
import com.example.banking_application.dto.AccountUpdateRequest;
import com.example.banking_application.exception.AccountNotFoundException;
import com.example.banking_application.exception.DuplicateAccountException;
import com.example.banking_application.exception.InvalidAccountStateException;
import com.example.banking_application.model.Account;
import com.example.banking_application.model.AccountStatus;
import com.example.banking_application.model.AccountType;
import com.example.banking_application.model.Role;
import com.example.banking_application.model.User;
import com.example.banking_application.repository.AccountRepository;
import com.example.banking_application.security.AccountAuthorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountAuthorizationService accountAuthorizationService;

    @InjectMocks
    private AccountService accountService;

    @Test
    void createAccount_shouldCreateAccountSuccessfully() {

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setName("Abhishek");
        currentUser.setEmail("test@gmail.com");
        currentUser.setRole(Role.USER);

        AccountRequest request = new AccountRequest();
        request.setAccountNumber("ACC123456");
        request.setAccountType(AccountType.SAVINGS);

        when(accountAuthorizationService.getCurrentUser())
                .thenReturn(currentUser);

        when(accountRepository.existsByAccountNumber(
                "ACC123456"
        )).thenReturn(false);

        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> {

                    Account account =
                            invocation.getArgument(0);

                    account.setId(1L);

                    return account;
                });

        AccountResponse response =
                accountService.createAccount(request);

        assertNotNull(response);

        assertEquals(
                1L,
                response.getId()
        );

        assertEquals(
                "ACC123456",
                response.getAccountNumber()
        );

        assertEquals(
                AccountType.SAVINGS,
                response.getAccountType()
        );

        assertEquals(
                BigDecimal.ZERO,
                response.getBalance()
        );

        assertEquals(
                AccountStatus.ACTIVE,
                response.getStatus()
        );

        assertEquals(
                1L,
                response.getUserId()
        );

        ArgumentCaptor<Account> accountCaptor =
                ArgumentCaptor.forClass(Account.class);

        verify(accountRepository)
                .save(accountCaptor.capture());

        Account savedAccount =
                accountCaptor.getValue();

        assertEquals(
                BigDecimal.ZERO,
                savedAccount.getBalance()
        );

        assertEquals(
                AccountStatus.ACTIVE,
                savedAccount.getStatus()
        );

        assertEquals(
                currentUser,
                savedAccount.getUser()
        );

        verify(
                accountRepository,
                times(1)
        ).existsByAccountNumber(
                "ACC123456"
        );
    }

    @Test
    void createAccount_shouldThrowExceptionWhenAccountNumberAlreadyExists() {

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setRole(Role.USER);

        AccountRequest request = new AccountRequest();
        request.setAccountNumber("ACC123456");
        request.setAccountType(AccountType.SAVINGS);

        when(accountAuthorizationService.getCurrentUser())
                .thenReturn(currentUser);

        when(accountRepository.existsByAccountNumber(
                "ACC123456"
        )).thenReturn(true);

        DuplicateAccountException exception =
                assertThrows(
                        DuplicateAccountException.class,
                        () -> accountService.createAccount(request)
                );

        assertEquals(
                "Account number already exists: ACC123456",
                exception.getMessage()
        );

        verify(
                accountRepository,
                never()
        ).save(any(Account.class));
    }

    @Test
    void getAccountById_shouldReturnAccountSuccessfully() {

        User user = new User();
        user.setId(1L);

        Account account = new Account();
        account.setId(10L);
        account.setAccountNumber("ACC123456");
        account.setAccountType(AccountType.SAVINGS);
        account.setBalance(
                new BigDecimal("5000.00")
        );
        account.setStatus(
                AccountStatus.ACTIVE
        );
        account.setUser(user);

        when(accountRepository.findById(10L))
                .thenReturn(
                        Optional.of(account)
                );

        AccountResponse response =
                accountService.getAccountById(10L);

        assertNotNull(response);

        assertEquals(
                10L,
                response.getId()
        );

        assertEquals(
                "ACC123456",
                response.getAccountNumber()
        );

        assertEquals(
                new BigDecimal("5000.00"),
                response.getBalance()
        );

        verify(
                accountAuthorizationService,
                times(1)
        ).checkAccountOwnership(account);
    }

    @Test
    void getAccountById_shouldThrowExceptionWhenAccountDoesNotExist() {

        when(accountRepository.findById(99L))
                .thenReturn(
                        Optional.empty()
                );

        AccountNotFoundException exception =
                assertThrows(
                        AccountNotFoundException.class,
                        () -> accountService.getAccountById(99L)
                );

        assertEquals(
                "Account with ID 99 not found",
                exception.getMessage()
        );

        verify(
                accountAuthorizationService,
                never()
        ).checkAccountOwnership(any(Account.class));
    }

    @Test
    void updateAccount_shouldUpdateAccountTypeSuccessfully() {

        User user = new User();
        user.setId(1L);

        Account existingAccount = new Account();
        existingAccount.setId(10L);
        existingAccount.setAccountNumber("ACC123456");
        existingAccount.setAccountType(AccountType.SAVINGS);
        existingAccount.setBalance(
                BigDecimal.ZERO
        );
        existingAccount.setStatus(
                AccountStatus.ACTIVE
        );
        existingAccount.setUser(user);

        AccountUpdateRequest request =
                new AccountUpdateRequest();

        request.setAccountType(
                AccountType.CURRENT
        );

        when(accountRepository.findById(10L))
                .thenReturn(
                        Optional.of(existingAccount)
                );

        when(accountRepository.save(existingAccount))
                .thenReturn(existingAccount);

        AccountResponse response =
                accountService.updateAccount(
                        10L,
                        request
                );

        assertNotNull(response);

        assertEquals(
                AccountType.CURRENT,
                response.getAccountType()
        );

        assertEquals(
                "ACC123456",
                response.getAccountNumber()
        );

        assertEquals(
                BigDecimal.ZERO,
                response.getBalance()
        );

        verify(
                accountRepository,
                times(1)
        ).save(existingAccount);
    }

    @Test
    void updateAccountStatus_shouldChangeActiveAccountToBlocked() {

        User user = new User();
        user.setId(1L);

        Account account = new Account();
        account.setId(10L);
        account.setAccountNumber("ACC123456");
        account.setAccountType(AccountType.SAVINGS);
        account.setBalance(
                BigDecimal.ZERO
        );
        account.setStatus(
                AccountStatus.ACTIVE
        );
        account.setUser(user);

        AccountStatusRequest request =
                new AccountStatusRequest();

        request.setStatus(
                AccountStatus.BLOCKED
        );

        when(accountRepository.findByIdForUpdate(10L))
                .thenReturn(
                        Optional.of(account)
                );

        when(accountRepository.save(account))
                .thenReturn(account);

        AccountResponse response =
                accountService.updateAccountStatus(
                        10L,
                        request
                );

        assertNotNull(response);

        assertEquals(
                AccountStatus.BLOCKED,
                response.getStatus()
        );

        verify(
                accountRepository,
                times(1)
        ).save(account);
    }

    @Test
    void updateAccountStatus_shouldThrowExceptionWhenClosingAccountWithNonZeroBalance() {

        User user = new User();
        user.setId(1L);

        Account account = new Account();
        account.setId(10L);
        account.setAccountNumber("ACC123456");
        account.setAccountType(AccountType.SAVINGS);
        account.setBalance(
                new BigDecimal("500.00")
        );
        account.setStatus(
                AccountStatus.ACTIVE
        );
        account.setUser(user);

        AccountStatusRequest request =
                new AccountStatusRequest();

        request.setStatus(
                AccountStatus.CLOSED
        );

        when(accountRepository.findByIdForUpdate(10L))
                .thenReturn(
                        Optional.of(account)
                );

        InvalidAccountStateException exception =
                assertThrows(
                        InvalidAccountStateException.class,
                        () -> accountService.updateAccountStatus(
                                10L,
                                request
                        )
                );

        assertEquals(
                "Account balance must be zero before closing",
                exception.getMessage()
        );

        verify(
                accountRepository,
                never()
        ).save(any(Account.class));
    }

    @Test
    void updateAccountStatus_shouldThrowExceptionWhenClosedAccountIsModified() {

        User user = new User();
        user.setId(1L);

        Account account = new Account();
        account.setId(10L);
        account.setAccountNumber("ACC123456");
        account.setAccountType(AccountType.SAVINGS);
        account.setBalance(
                BigDecimal.ZERO
        );
        account.setStatus(
                AccountStatus.CLOSED
        );
        account.setUser(user);

        AccountStatusRequest request =
                new AccountStatusRequest();

        request.setStatus(
                AccountStatus.ACTIVE
        );

        when(accountRepository.findByIdForUpdate(10L))
                .thenReturn(
                        Optional.of(account)
                );

        InvalidAccountStateException exception =
                assertThrows(
                        InvalidAccountStateException.class,
                        () -> accountService.updateAccountStatus(
                                10L,
                                request
                        )
                );

        assertEquals(
                "A closed account cannot be reopened or modified",
                exception.getMessage()
        );

        verify(
                accountRepository,
                never()
        ).save(any(Account.class));
    }
}