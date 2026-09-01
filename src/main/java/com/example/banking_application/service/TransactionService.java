package com.example.banking_application.service;

import com.example.banking_application.dto.*;
import com.example.banking_application.exception.AccountNotFoundException;
import com.example.banking_application.exception.InsufficientBalanceException;
import com.example.banking_application.exception.InvalidAccountStateException;
import com.example.banking_application.exception.InvalidTransferException;
import com.example.banking_application.model.Account;
import com.example.banking_application.model.AccountStatus;
import com.example.banking_application.model.Transaction;
import com.example.banking_application.model.TransactionType;
import com.example.banking_application.repository.AccountRepository;
import com.example.banking_application.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.banking_application.security.AccountAuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountAuthorizationService accountAuthorizationService;

    public TransactionService(AccountRepository accountRepository, TransactionRepository transactionRepository, AccountAuthorizationService accountAuthorizationService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.accountAuthorizationService = accountAuthorizationService;
    }

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private AccountResponse convertToResponse(Account account) {

        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getBalance(),
                account.getStatus(),
                account.getUser().getId()
        );
    }

    private void validateAccountForTransaction(Account account) {

        if (account.getStatus() == AccountStatus.BLOCKED) {
            throw new InvalidAccountStateException(
                    "Transactions are not allowed on a blocked account"
            );
        }

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new InvalidAccountStateException(
                    "Transactions are not allowed on a closed account"
            );
        }
    }

    @Transactional
    public AccountResponse deposit(Long accountId, TransactionRequest request) {

        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Account not found with id: " + accountId
                        )
                );

        accountAuthorizationService.checkAccountOwnership(account);

        validateAccountForTransaction(account);

        BigDecimal newBalance = account.getBalance().add(request.getAmount());

        account.setBalance(newBalance);

        accountRepository.save(account);

        Transaction transaction = new Transaction();

        transaction.setAccount(account);
        transaction.setTransactionType(TransactionType.DEPOSIT);
        transaction.setAmount(request.getAmount());
        transaction.setTransactionDate(LocalDateTime.now());

        transactionRepository.save(transaction);

        logger.info("Deposit completed for accountId={}", accountId);

        return convertToResponse(account);
    }

    @Transactional
    public AccountResponse withdraw(Long accountId, TransactionRequest request) {

        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Account not found with id: " + accountId
                        )
                );

        accountAuthorizationService.checkAccountOwnership(account);

        validateAccountForTransaction(account);

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance in the account"
            );
        }

        BigDecimal newBalance = account.getBalance().subtract(request.getAmount());

        account.setBalance(newBalance);

        accountRepository.save(account);

        Transaction transaction = new Transaction();

        transaction.setAccount(account);
        transaction.setTransactionType(TransactionType.WITHDRAWAL);
        transaction.setAmount(request.getAmount());
        transaction.setTransactionDate(LocalDateTime.now());

        transactionRepository.save(transaction);

        logger.info("Withdrawal completed for accountId={}", accountId);

        return convertToResponse(account);
    }

    private TransactionResponse convertTransactionToResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getTransactionDate()
        );
    }

    @Transactional
    public void transfer(Long senderId, TransferRequest request) {

        Long receiverId = request.getReceiverAccountId();

        // Reject same-account transfer before taking database locks
        if (senderId.equals(receiverId)) {
            throw new InvalidTransferException(
                    "Sender and receiver accounts cannot be the same"
            );
        }

        // Load sender normally first for ownership authorization
        Account senderForAuthorization = accountRepository.findById(senderId)
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Sender account not found with id: " + senderId
                        )
                );

        accountAuthorizationService.checkAccountOwnership(
                senderForAuthorization
        );

        // Always lock accounts in deterministic ID order
        Long firstAccountId = Math.min(senderId, receiverId);
        Long secondAccountId = Math.max(senderId, receiverId);

        Account firstAccount = accountRepository.findByIdForUpdate(firstAccountId)
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Account not found with id: " + firstAccountId
                        )
                );

        Account secondAccount = accountRepository.findByIdForUpdate(secondAccountId)
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Account not found with id: " + secondAccountId
                        )
                );

        // Identify sender and receiver after locking
        Account sender =
                senderId.equals(firstAccountId)
                        ? firstAccount
                        : secondAccount;

        Account receiver =
                receiverId.equals(firstAccountId)
                        ? firstAccount
                        : secondAccount;

        // Validate current account states after locks are acquired
        validateAccountForTransaction(sender);
        validateAccountForTransaction(receiver);

        if (sender.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance in the account"
            );
        }

        // Update balances
        sender.setBalance(
                sender.getBalance()
                        .subtract(request.getAmount())
        );

        receiver.setBalance(
                receiver.getBalance()
                        .add(request.getAmount())
        );

        // Create sender transaction record
        Transaction senderTransaction = new Transaction();

        senderTransaction.setAccount(sender);
        senderTransaction.setTransactionType(
                TransactionType.TRANSFER_OUT
        );
        senderTransaction.setAmount(
                request.getAmount()
        );
        senderTransaction.setTransactionDate(
                LocalDateTime.now()
        );

        // Create receiver transaction record
        Transaction receiverTransaction = new Transaction();

        receiverTransaction.setAccount(receiver);
        receiverTransaction.setTransactionType(
                TransactionType.TRANSFER_IN
        );
        receiverTransaction.setAmount(
                request.getAmount()
        );
        receiverTransaction.setTransactionDate(
                LocalDateTime.now()
        );

        accountRepository.save(sender);
        accountRepository.save(receiver);

        transactionRepository.save(senderTransaction);
        transactionRepository.save(receiverTransaction);

        logger.info("Transfer completed from accountId={} to accountId={}", senderId, request.getReceiverAccountId());
    }

    public TransactionPageResponse getTransactionHistory(
            Long accountId,
            TransactionType type,
            Pageable pageable) {

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Account not found with id: " + accountId
                        )
                );

        accountAuthorizationService.checkAccountOwnership(account);

        Page<Transaction> transactionPage;

        if (type == null) {

            transactionPage = transactionRepository
                    .findByAccountId(accountId, pageable);

        } else {

            transactionPage = transactionRepository
                    .findByAccountIdAndTransactionType(
                            accountId,
                            type,
                            pageable
                    );
        }

        List<TransactionResponse> transactions =
                transactionPage.getContent()
                        .stream()
                        .map(this::convertTransactionToResponse)
                        .toList();

        return new TransactionPageResponse(
                transactions,
                transactionPage.getNumber(),
                transactionPage.getSize(),
                transactionPage.getTotalElements(),
                transactionPage.getTotalPages(),
                transactionPage.isFirst(),
                transactionPage.isLast()
        );
    }
}
