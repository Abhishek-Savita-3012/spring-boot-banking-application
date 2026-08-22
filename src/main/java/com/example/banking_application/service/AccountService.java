package com.example.banking_application.service;

import com.example.banking_application.exception.AccountNotFoundException;
import com.example.banking_application.model.Account;
import com.example.banking_application.repository.AccountRepository;
import org.springframework.stereotype.Service;
import com.example.banking_application.dto.AccountRequest;
import com.example.banking_application.dto.AccountResponse;

import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
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

        Account account = new Account();

        account.setAccountNumber(request.getAccountNumber());
        account.setAccountHolderName(request.getAccountHolderName());
        account.setAccountType(request.getAccountType());
        account.setBalance(request.getBalance());
        account.setEmail(request.getEmail());

        Account savedAccount = accountRepository.save(account);

        return convertToResponse(savedAccount);
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public Account getAccountById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() ->
                    new AccountNotFoundException("Account with ID " + id + " not found")
                );
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
}
