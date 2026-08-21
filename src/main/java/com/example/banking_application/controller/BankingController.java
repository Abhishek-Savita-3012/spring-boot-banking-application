package com.example.banking_application.controller;

import com.example.banking_application.model.Account;
import com.example.banking_application.service.AccountService;
import com.example.banking_application.service.BankingService;
import com.example.banking_application.service.CustomerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BankingController {

    private final BankingService bankingService;
    private final CustomerService customerService;
    private final AccountService accountService;

    public BankingController(BankingService bankingService, CustomerService customerService, AccountService accountService) {
        this.bankingService = bankingService;
        this.customerService = customerService;
        this.accountService = accountService;
    }

    @GetMapping("/hello")
    public String hello(){
        return "Welcome to Banking Application";
    }

    @GetMapping("/bank/status")
    public String bankStatus(){
        return bankingService.getBankStatus();
    }

    @GetMapping("/customers")
    public String customers(){
        return customerService.getCustomerMessage();
    }

    @GetMapping("/accounts/sample")
    public Account getSampleAccounts(){
        return accountService.createSampleAccount();
    }

    @GetMapping("accounts/summary")
    public String getAccountSummary(){
        return accountService.getSummary();
    }
}
