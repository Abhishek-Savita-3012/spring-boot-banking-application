package com.example.banking_application.service;

import com.example.banking_application.dto.*;
import com.example.banking_application.exception.*;
import com.example.banking_application.model.Account;
import com.example.banking_application.model.User;
import com.example.banking_application.repository.AccountRepository;
import com.example.banking_application.repository.TransactionRepository;
import com.example.banking_application.repository.UserRepository;
import org.springframework.stereotype.Service;
import com.example.banking_application.model.Transaction;
import com.example.banking_application.model.TransactionType;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public AccountService(AccountRepository accountRepository, TransactionRepository transactionRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
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

    public AccountResponse createAccount(AccountRequest request) {

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User with ID " + request.getUserId() + " not found"
                        )
                );

        if (accountRepository.existsByAccountNumber(request.getAccountNumber())) {
            throw new DuplicateAccountException(
                    "Account number already exists: "
                            + request.getAccountNumber()
            );
        }

        Account account = new Account();

        account.setAccountNumber(request.getAccountNumber());
        account.setAccountType(request.getAccountType());
        account.setBalance(request.getBalance());
        account.setUser(user);

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

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User with ID " + request.getUserId() + " not found"
                        )
                );

        existingAccount.setAccountNumber(
                request.getAccountNumber()
        );

        existingAccount.setAccountType(
                request.getAccountType()
        );

        existingAccount.setBalance(
                request.getBalance()
        );

        existingAccount.setUser(user);

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
}
