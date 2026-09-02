package com.example.banking_application.service;

import com.example.banking_application.dto.TransactionPageResponse;
import com.example.banking_application.dto.TransactionRequest;
import com.example.banking_application.dto.TransferRequest;
import com.example.banking_application.exception.AccountNotFoundException;
import com.example.banking_application.exception.InsufficientBalanceException;
import com.example.banking_application.exception.InvalidAccountStateException;
import com.example.banking_application.exception.InvalidTransferException;
import com.example.banking_application.model.Account;
import com.example.banking_application.model.AccountStatus;
import com.example.banking_application.model.AccountType;
import com.example.banking_application.model.Transaction;
import com.example.banking_application.model.TransactionType;
import com.example.banking_application.model.User;
import com.example.banking_application.repository.AccountRepository;
import com.example.banking_application.repository.TransactionRepository;
import com.example.banking_application.security.AccountAuthorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountAuthorizationService accountAuthorizationService;

    @InjectMocks
    private TransactionService transactionService;

    private Account createAccount(
            Long id,
            BigDecimal balance,
            AccountStatus status) {

        User user = new User();
        user.setId(1L);

        Account account = new Account();
        account.setId(id);
        account.setAccountNumber("ACC" + id);
        account.setAccountType(AccountType.SAVINGS);
        account.setBalance(balance);
        account.setStatus(status);
        account.setUser(user);

        return account;
    }

    @Test
    void deposit_shouldIncreaseBalanceAndCreateTransaction() {

        Account account = createAccount(
                10L,
                new BigDecimal("1000.00"),
                AccountStatus.ACTIVE
        );

        TransactionRequest request =
                new TransactionRequest();

        request.setAmount(
                new BigDecimal("500.00")
        );

        when(accountRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(account));

        when(accountRepository.save(account))
                .thenReturn(account);

        transactionService.deposit(
                10L,
                request
        );

        assertEquals(
                new BigDecimal("1500.00"),
                account.getBalance()
        );

        verify(
                accountAuthorizationService
        ).checkAccountOwnership(account);

        verify(
                accountRepository
        ).save(account);

        ArgumentCaptor<Transaction> captor =
                ArgumentCaptor.forClass(Transaction.class);

        verify(transactionRepository)
                .save(captor.capture());

        Transaction transaction =
                captor.getValue();

        assertEquals(
                TransactionType.DEPOSIT,
                transaction.getTransactionType()
        );

        assertEquals(
                new BigDecimal("500.00"),
                transaction.getAmount()
        );

        assertEquals(
                account,
                transaction.getAccount()
        );

        assertNotNull(
                transaction.getTransactionDate()
        );
    }

    @Test
    void deposit_shouldThrowExceptionWhenAccountIsBlocked() {

        Account account = createAccount(
                10L,
                new BigDecimal("1000.00"),
                AccountStatus.BLOCKED
        );

        TransactionRequest request =
                new TransactionRequest();

        request.setAmount(
                new BigDecimal("500.00")
        );

        when(accountRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(account));

        assertThrows(
                InvalidAccountStateException.class,
                () -> transactionService.deposit(
                        10L,
                        request
                )
        );

        assertEquals(
                new BigDecimal("1000.00"),
                account.getBalance()
        );

        verify(
                accountRepository,
                never()
        ).save(any(Account.class));

        verify(
                transactionRepository,
                never()
        ).save(any(Transaction.class));
    }

    @Test
    void withdraw_shouldDecreaseBalanceAndCreateTransaction() {

        Account account = createAccount(
                10L,
                new BigDecimal("1000.00"),
                AccountStatus.ACTIVE
        );

        TransactionRequest request =
                new TransactionRequest();

        request.setAmount(
                new BigDecimal("300.00")
        );

        when(accountRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(account));

        when(accountRepository.save(account))
                .thenReturn(account);

        transactionService.withdraw(
                10L,
                request
        );

        assertEquals(
                new BigDecimal("700.00"),
                account.getBalance()
        );

        ArgumentCaptor<Transaction> captor =
                ArgumentCaptor.forClass(Transaction.class);

        verify(transactionRepository)
                .save(captor.capture());

        Transaction transaction =
                captor.getValue();

        assertEquals(
                TransactionType.WITHDRAWAL,
                transaction.getTransactionType()
        );

        assertEquals(
                new BigDecimal("300.00"),
                transaction.getAmount()
        );
    }

    @Test
    void withdraw_shouldThrowExceptionWhenBalanceIsInsufficient() {

        Account account = createAccount(
                10L,
                new BigDecimal("200.00"),
                AccountStatus.ACTIVE
        );

        TransactionRequest request =
                new TransactionRequest();

        request.setAmount(
                new BigDecimal("500.00")
        );

        when(accountRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(account));

        InsufficientBalanceException exception =
                assertThrows(
                        InsufficientBalanceException.class,
                        () -> transactionService.withdraw(
                                10L,
                                request
                        )
                );

        assertEquals(
                "Insufficient balance in the account",
                exception.getMessage()
        );

        assertEquals(
                new BigDecimal("200.00"),
                account.getBalance()
        );

        verify(
                transactionRepository,
                never()
        ).save(any(Transaction.class));
    }

    @Test
    void transfer_shouldTransferMoneySuccessfully() {

        Account sender = createAccount(
                10L,
                new BigDecimal("1000.00"),
                AccountStatus.ACTIVE
        );

        Account receiver = createAccount(
                20L,
                new BigDecimal("500.00"),
                AccountStatus.ACTIVE
        );

        TransferRequest request =
                new TransferRequest();

        request.setReceiverAccountId(20L);
        request.setAmount(
                new BigDecimal("300.00")
        );

        when(accountRepository.findById(10L))
                .thenReturn(Optional.of(sender));

        when(accountRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(sender));

        when(accountRepository.findByIdForUpdate(20L))
                .thenReturn(Optional.of(receiver));

        transactionService.transfer(
                10L,
                request
        );

        assertEquals(
                new BigDecimal("700.00"),
                sender.getBalance()
        );

        assertEquals(
                new BigDecimal("800.00"),
                receiver.getBalance()
        );

        verify(
                accountAuthorizationService
        ).checkAccountOwnership(sender);

        verify(
                accountRepository
        ).save(sender);

        verify(
                accountRepository
        ).save(receiver);

        ArgumentCaptor<Transaction> captor =
                ArgumentCaptor.forClass(Transaction.class);

        verify(
                transactionRepository,
                times(2)
        ).save(captor.capture());

        List<Transaction> transactions =
                captor.getAllValues();

        assertEquals(
                TransactionType.TRANSFER_OUT,
                transactions.get(0).getTransactionType()
        );

        assertEquals(
                TransactionType.TRANSFER_IN,
                transactions.get(1).getTransactionType()
        );

        assertEquals(
                new BigDecimal("300.00"),
                transactions.get(0).getAmount()
        );

        assertEquals(
                new BigDecimal("300.00"),
                transactions.get(1).getAmount()
        );
    }

    @Test
    void transfer_shouldRejectSameAccountTransfer() {

        TransferRequest request =
                new TransferRequest();

        request.setReceiverAccountId(10L);
        request.setAmount(
                new BigDecimal("100.00")
        );

        InvalidTransferException exception =
                assertThrows(
                        InvalidTransferException.class,
                        () -> transactionService.transfer(
                                10L,
                                request
                        )
                );

        assertEquals(
                "Sender and receiver accounts cannot be the same",
                exception.getMessage()
        );

        verifyNoInteractions(
                accountRepository,
                transactionRepository,
                accountAuthorizationService
        );
    }

    @Test
    void transfer_shouldThrowExceptionWhenSenderHasInsufficientBalance() {

        Account sender = createAccount(
                10L,
                new BigDecimal("100.00"),
                AccountStatus.ACTIVE
        );

        Account receiver = createAccount(
                20L,
                new BigDecimal("500.00"),
                AccountStatus.ACTIVE
        );

        TransferRequest request =
                new TransferRequest();

        request.setReceiverAccountId(20L);
        request.setAmount(
                new BigDecimal("300.00")
        );

        when(accountRepository.findById(10L))
                .thenReturn(Optional.of(sender));

        when(accountRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(sender));

        when(accountRepository.findByIdForUpdate(20L))
                .thenReturn(Optional.of(receiver));

        assertThrows(
                InsufficientBalanceException.class,
                () -> transactionService.transfer(
                        10L,
                        request
                )
        );

        assertEquals(
                new BigDecimal("100.00"),
                sender.getBalance()
        );

        assertEquals(
                new BigDecimal("500.00"),
                receiver.getBalance()
        );

        verify(
                transactionRepository,
                never()
        ).save(any(Transaction.class));
    }

    @Test
    void transfer_shouldRejectBlockedReceiver() {

        Account sender = createAccount(
                10L,
                new BigDecimal("1000.00"),
                AccountStatus.ACTIVE
        );

        Account receiver = createAccount(
                20L,
                new BigDecimal("500.00"),
                AccountStatus.BLOCKED
        );

        TransferRequest request =
                new TransferRequest();

        request.setReceiverAccountId(20L);
        request.setAmount(
                new BigDecimal("200.00")
        );

        when(accountRepository.findById(10L))
                .thenReturn(Optional.of(sender));

        when(accountRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(sender));

        when(accountRepository.findByIdForUpdate(20L))
                .thenReturn(Optional.of(receiver));

        assertThrows(
                InvalidAccountStateException.class,
                () -> transactionService.transfer(
                        10L,
                        request
                )
        );

        assertEquals(
                new BigDecimal("1000.00"),
                sender.getBalance()
        );

        assertEquals(
                new BigDecimal("500.00"),
                receiver.getBalance()
        );

        verify(
                transactionRepository,
                never()
        ).save(any(Transaction.class));
    }

    @Test
    void transfer_shouldLockAccountsInAscendingIdOrder() {

        Account sender = createAccount(
                20L,
                new BigDecimal("1000.00"),
                AccountStatus.ACTIVE
        );

        Account receiver = createAccount(
                10L,
                new BigDecimal("500.00"),
                AccountStatus.ACTIVE
        );

        TransferRequest request =
                new TransferRequest();

        request.setReceiverAccountId(10L);
        request.setAmount(
                new BigDecimal("100.00")
        );

        when(accountRepository.findById(20L))
                .thenReturn(Optional.of(sender));

        when(accountRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(receiver));

        when(accountRepository.findByIdForUpdate(20L))
                .thenReturn(Optional.of(sender));

        transactionService.transfer(
                20L,
                request
        );

        InOrder inOrder =
                inOrder(accountRepository);

        inOrder.verify(accountRepository)
                .findById(20L);

        inOrder.verify(accountRepository)
                .findByIdForUpdate(10L);

        inOrder.verify(accountRepository)
                .findByIdForUpdate(20L);

        assertEquals(
                new BigDecimal("900.00"),
                sender.getBalance()
        );

        assertEquals(
                new BigDecimal("600.00"),
                receiver.getBalance()
        );
    }

    @Test
    void getTransactionHistory_shouldReturnPagedTransactions() {

        Account account = createAccount(
                10L,
                new BigDecimal("1000.00"),
                AccountStatus.ACTIVE
        );

        Transaction transaction = new Transaction();
        transaction.setId(1L);
        transaction.setAccount(account);
        transaction.setTransactionType(
                TransactionType.DEPOSIT
        );
        transaction.setAmount(
                new BigDecimal("500.00")
        );
        transaction.setTransactionDate(
                LocalDateTime.now()
        );

        Pageable pageable =
                PageRequest.of(0, 10);

        Page<Transaction> page =
                new PageImpl<>(
                        List.of(transaction),
                        pageable,
                        1
                );

        when(accountRepository.findById(10L))
                .thenReturn(Optional.of(account));

        when(transactionRepository.findByAccountId(
                10L,
                pageable
        )).thenReturn(page);

        TransactionPageResponse response =
                transactionService.getTransactionHistory(
                        10L,
                        null,
                        pageable
                );

        assertNotNull(response);

        assertEquals(
                1,
                response.getTransactions().size()
        );

        assertEquals(
                0,
                response.getCurrentPage()
        );

        assertEquals(
                10,
                response.getPageSize()
        );

        assertEquals(
                1,
                response.getTotalElements()
        );

        assertEquals(
                TransactionType.DEPOSIT,
                response.getTransactions()
                        .get(0)
                        .getTransactionType()
        );

        verify(
                accountAuthorizationService
        ).checkAccountOwnership(account);

        verify(
                transactionRepository
        ).findByAccountId(
                10L,
                pageable
        );
    }

    @Test
    void getTransactionHistory_shouldFilterByTransactionType() {

        Account account = createAccount(
                10L,
                new BigDecimal("1000.00"),
                AccountStatus.ACTIVE
        );

        Transaction transaction =
                new Transaction();

        transaction.setId(1L);
        transaction.setAccount(account);
        transaction.setTransactionType(
                TransactionType.TRANSFER_OUT
        );
        transaction.setAmount(
                new BigDecimal("100.00")
        );
        transaction.setTransactionDate(
                LocalDateTime.now()
        );

        Pageable pageable =
                PageRequest.of(0, 10);

        Page<Transaction> page =
                new PageImpl<>(
                        List.of(transaction),
                        pageable,
                        1
                );

        when(accountRepository.findById(10L))
                .thenReturn(Optional.of(account));

        when(transactionRepository
                .findByAccountIdAndTransactionType(
                        10L,
                        TransactionType.TRANSFER_OUT,
                        pageable
                ))
                .thenReturn(page);

        TransactionPageResponse response =
                transactionService.getTransactionHistory(
                        10L,
                        TransactionType.TRANSFER_OUT,
                        pageable
                );

        assertEquals(
                1,
                response.getTransactions().size()
        );

        assertEquals(
                TransactionType.TRANSFER_OUT,
                response.getTransactions()
                        .get(0)
                        .getTransactionType()
        );

        verify(
                transactionRepository
        ).findByAccountIdAndTransactionType(
                10L,
                TransactionType.TRANSFER_OUT,
                pageable
        );

        verify(
                transactionRepository,
                never()
        ).findByAccountId(
                anyLong(),
                any(Pageable.class)
        );
    }
}