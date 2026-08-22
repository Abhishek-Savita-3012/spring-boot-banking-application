package com.example.banking_application.service;

import com.example.banking_application.exception.AccountNotFoundException;
import com.example.banking_application.model.Account;
import com.example.banking_application.model.AccountType;
import com.example.banking_application.repository.AccountRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Account createAccount(Account account){
        return accountRepository.save(account);
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

    public Account updateAccount(Long id, Account updatedAccount) {

        Account existingAccount = accountRepository.findById(id)
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Account with ID " + id + " not found"
                        )
                );

        existingAccount.setAccountNumber(
                updatedAccount.getAccountNumber()
        );

        existingAccount.setAccountHolderName(
                updatedAccount.getAccountHolderName()
        );

        existingAccount.setAccountType(
                updatedAccount.getAccountType()
        );

        existingAccount.setBalance(
                updatedAccount.getBalance()
        );

        existingAccount.setEmail(
                updatedAccount.getEmail()
        );

        return accountRepository.save(existingAccount);
    }

    public void deleteAccount(Long id) {

        Account account = accountRepository.findById(id)
                .orElseThrow(() ->
                        new AccountNotFoundException("Account with ID " + id + " not found")
                );

        accountRepository.delete(account);
    }
}
