package com.example.banking_application.service;

import com.example.banking_application.dto.*;
import com.example.banking_application.exception.AccountNotFoundException;
import com.example.banking_application.exception.InsufficientBalanceException;
import com.example.banking_application.exception.InvalidTransferException;
import com.example.banking_application.model.Account;
import com.example.banking_application.model.Transaction;
import com.example.banking_application.model.TransactionType;
import com.example.banking_application.repository.AccountRepository;
import com.example.banking_application.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    private AccountResponse convertToResponse(Account account) {

        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getBalance(),
                account.getUser().getId()
        );
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

        Account sender = accountRepository.findByIdForUpdate(senderId)
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Sender account not found with id: " + senderId
                        )
                );

        Account receiver = accountRepository.findByIdForUpdate(request.getReceiverAccountId())
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Receiver account not found with id: " + request.getReceiverAccountId()
                        )
                );

        if (sender.getId().equals(receiver.getId())) {
            throw new InvalidTransferException(
                    "Sender and receiver accounts cannot be the same"
            );
        }

        if (sender.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance in the account"
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
