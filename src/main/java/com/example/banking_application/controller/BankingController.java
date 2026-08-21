package com.example.banking_application.controller;

import com.example.banking_application.model.Account;
import com.example.banking_application.service.AccountService;
import com.example.banking_application.service.BankingService;
import com.example.banking_application.service.CustomerService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
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

    @GetMapping("accounts/summary")
    public String getAccountSummary(){
        return accountService.getSummary();
    }

    @GetMapping("/accounts")
    public Account getSampleAccounts(){
        return accountService.createSampleAccount();
    }

    @GetMapping("/accounts/{id}")
    public String getAccountById(@PathVariable String id){
        return "Requested account ID: " + id;
    }

    @GetMapping("/accounts/{id}/summary")
    public String getAccountSummary(@PathVariable String id){
        return "Account summary requested for account ID: " + id;
    }

    @PostMapping("/accounts")
    public Account createAccount(@RequestBody Account account){
        return accountService.createAccount(account);
    }

    @PostMapping("/accounts/test")
    public Account createAccountTest(@RequestBody Account account){
        return accountService.createAccount(account);
    }

    @PutMapping("/accounts/{id}")
    public Account updateAccount(@PathVariable Long id, @RequestBody Account account){
        account.setId(id);

        return account;
    }

    @DeleteMapping("/accounts/{id}")
    public String deleteAccount(@PathVariable Long id){
        return "Account " + id + " deleted successfully";
    }

    @DeleteMapping("/accounts/{id}/test")
    public String deleteAccountTest(@PathVariable Long id){
        return "Test deletion for account: " + id;
    }
}
