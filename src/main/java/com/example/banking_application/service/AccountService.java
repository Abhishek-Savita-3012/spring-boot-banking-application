package com.example.banking_application.service;

import com.example.banking_application.model.Account;
import com.example.banking_application.model.AccountType;
import org.springframework.stereotype.Service;

@Service
public class AccountService {
    public Account createSampleAccount(){
        Account account = new Account();

        account.setId(1L);
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
        return account;
    }
}
