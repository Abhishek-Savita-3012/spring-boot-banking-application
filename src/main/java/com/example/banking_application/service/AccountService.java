package com.example.banking_application.service;

import com.example.banking_application.dto.*;
import com.example.banking_application.exception.AccountNotFoundException;
import com.example.banking_application.exception.DuplicateAccountException;
import com.example.banking_application.exception.InsufficientBalanceException;
import com.example.banking_application.model.Account;
import com.example.banking_application.repository.AccountRepository;
import com.example.banking_application.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import com.example.banking_application.model.Transaction;
import com.example.banking_application.model.TransactionType;
import org.springframework.transaction.annotation.Transactional;
import com.example.banking_application.exception.InvalidTransferException;
import com.example.banking_application.dto.TransactionPageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;


import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AccountService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    private AccountResponse convertToResponse(Account account) {

        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountHolderName(),
                account.getAccountType(),
                account.getBalance(),
                account.getEmail()
        );
    }

    public AccountResponse createAccount(AccountRequest request) {

        if (accountRepository.existsByAccountNumber(request.getAccountNumber())) {
            throw new DuplicateAccountException(
                    "Account number already exists: "
                            + request.getAccountNumber()
            );
        }

        Account account = new Account();

        account.setAccountNumber(request.getAccountNumber());
        account.setAccountHolderName(request.getAccountHolderName());
        account.setAccountType(request.getAccountType());
        account.setBalance(request.getBalance());
        account.setEmail(request.getEmail());

        Account savedAccount = accountRepository.save(account);

        return convertToResponse(savedAccount);
    }

    public List<AccountResponse> getAllAccounts() {

        return accountRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    public AccountResponse getAccountById(Long id) {

        Account account = accountRepository.findById(id)
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Account with ID " + id + " not found"
                        )
                );

        return convertToResponse(account);
    }

    public AccountResponse updateAccount(Long id, AccountRequest request) {

        Account existingAccount = accountRepository.findById(id)
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Account with ID " + id + " not found"
                        )
                );

        existingAccount.setAccountNumber(
                request.getAccountNumber()
        );

        existingAccount.setAccountHolderName(
                request.getAccountHolderName()
        );

        existingAccount.setAccountType(
                request.getAccountType()
        );

        existingAccount.setBalance(
                request.getBalance()
        );

        existingAccount.setEmail(
                request.getEmail()
        );

        Account savedAccount = accountRepository.save(existingAccount);

        return convertToResponse(savedAccount);
    }

    public void deleteAccount(Long id) {

        Account account = accountRepository.findById(id)
                .orElseThrow(() ->
                        new AccountNotFoundException("Account with ID " + id + " not found")
                );

        accountRepository.delete(account);
    }

    @Transactional
    public AccountResponse deposit(Long accountId, TransactionRequest request) {

        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Account not found with id: " + accountId
                        )
                );

        BigDecimal newBalance = account.getBalance().add(request.getAmount());

        account.setBalance(newBalance);

        accountRepository.save(account);

        Transaction transaction = new Transaction();

        transaction.setAccount(account);
        transaction.setTransactionType(TransactionType.DEPOSIT);
        transaction.setAmount(request.getAmount());
        transaction.setTransactionDate(LocalDateTime.now());

        transactionRepository.save(transaction);

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

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance"
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

//    public List<TransactionResponse> getTransactionHistory(Long accountId, TransactionType type) {
//
//        accountRepository.findById(accountId)
//                .orElseThrow(() ->
//                        new AccountNotFoundException(
//                                "Account not found with id: " + accountId
//                        )
//                );
//
//        List<Transaction> transactions;
//
//        if (type == null) {
//            transactions = transactionRepository
//                    .findByAccountIdOrderByTransactionDateDesc(
//                            accountId
//                    );
//
//        } else {
//            transactions = transactionRepository
//                    .findByAccountIdAndTransactionTypeOrderByTransactionDateDesc(
//                            accountId,
//                            type
//                    );
//        }
//
//        return transactions.stream()
//                .map(this::convertTransactionToResponse)
//                .toList();
//    }

    @Transactional
    public void transfer(Long senderId, TransferRequest request) {

        Account sender = accountRepository.findByIdForUpdate(senderId)
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Sender account not found with id: " + senderId
                        )
                );

        Account receiver = accountRepository.findByIdForUpdate(request.getReceiverAccountId())
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Receiver account not found with id: "
                                + request.getReceiverAccountId()
                )
        );

        if (sender.getId().equals(receiver.getId())) {
            throw new InvalidTransferException(
                    "Sender and receiver accounts cannot be the same"
            );
        }

        if (sender.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance"
            );
        }

        sender.setBalance(sender.getBalance().subtract(request.getAmount()));

        receiver.setBalance(receiver.getBalance().add(request.getAmount()));

        Transaction senderTransaction = new Transaction();

        senderTransaction.setAccount(sender);
        senderTransaction.setTransactionType(TransactionType.WITHDRAWAL);
        senderTransaction.setAmount(request.getAmount());
        senderTransaction.setTransactionDate(LocalDateTime.now());

        Transaction receiverTransaction = new Transaction();

        receiverTransaction.setAccount(receiver);
        receiverTransaction.setTransactionType(TransactionType.DEPOSIT);
        receiverTransaction.setAmount(request.getAmount());
        receiverTransaction.setTransactionDate(LocalDateTime.now());

        accountRepository.save(sender);
        accountRepository.save(receiver);

        transactionRepository.save(senderTransaction);
        transactionRepository.save(receiverTransaction);
    }

    public TransactionPageResponse getTransactionHistory(
            Long accountId,
            TransactionType type,
            Pageable pageable) {

        accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Account not found with id: " + accountId
                        )
                );

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
