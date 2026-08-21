package com.example.banking_application.service;

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

    public Account createSampleAccount(){
        Account account = new Account();
        account.setAccountNumber("AC100001");
        account.setAccountHolderName("Abhishek");
        account.setAccountType(AccountType.SAVINGS);
        account.setBalance(10000.00);
        account.setEmail("abc@gmail.com");

        return account;
    }

    public String getSummary(){
        Account account = createSampleAccount();
        return "Account " + account.getAccountNumber() + " belongs to " + account.getAccountHolderName()
                + " and has a balance of " + account.getBalance();
    }

    public Account createAccount(Account account){
        return accountRepository.save(account);
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public Account getAccountById(Long id) {
        return accountRepository.findById(id).orElse(null);
    }
}
