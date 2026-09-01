package com.example.banking_application.service;

import com.example.banking_application.dto.*;
import com.example.banking_application.exception.*;
import com.example.banking_application.model.Account;
import com.example.banking_application.model.AccountStatus;
import com.example.banking_application.model.User;
import com.example.banking_application.repository.AccountRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.example.banking_application.security.AccountAuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountAuthorizationService accountAuthorizationService;

    public AccountService(AccountRepository accountRepository, AccountAuthorizationService accountAuthorizationService) {
        this.accountRepository = accountRepository;
        this.accountAuthorizationService = accountAuthorizationService;
    }

    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);

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

    public AccountResponse createAccount(AccountRequest request) {

        User currentUser = accountAuthorizationService.getCurrentUser();

        if (accountRepository.existsByAccountNumber(request.getAccountNumber())) {
            throw new DuplicateAccountException(
                    "Account number already exists: " + request.getAccountNumber()
            );
        }

        Account account = new Account();

        account.setAccountNumber(
                request.getAccountNumber()
        );

        account.setAccountType(
                request.getAccountType()
        );

        account.setBalance(
                BigDecimal.ZERO
        );

        account.setStatus(
                AccountStatus.ACTIVE
        );

        account.setUser(
                currentUser
        );

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

        accountAuthorizationService.checkAccountOwnership(account);

        return convertToResponse(account);
    }

    public AccountResponse updateAccount(Long id, AccountUpdateRequest request) {

        Account existingAccount = accountRepository.findById(id)
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Account with ID " + id + " not found"
                        )
                );

        existingAccount.setAccountType(request.getAccountType());

        Account savedAccount = accountRepository.save(existingAccount);

        return convertToResponse(savedAccount);
    }

    @Transactional
    public AccountResponse updateAccountStatus(Long id, AccountStatusRequest request) {

        Account account = accountRepository.findByIdForUpdate(id)
                                .orElseThrow(() ->
                                        new AccountNotFoundException(
                                                "Account with ID " + id + " not found"
                                        )
                                );

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new InvalidAccountStateException(
                    "A closed account cannot be reopened or modified"
            );
        }

        if (request.getStatus() == AccountStatus.CLOSED && account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new InvalidAccountStateException(
                    "Account balance must be zero before closing"
            );
        }

        account.setStatus(request.getStatus());

        Account savedAccount = accountRepository.save(account);

        logger.info("Account status changed accountId={} status={}", id, request.getStatus());

        return convertToResponse(savedAccount);
    }
}
