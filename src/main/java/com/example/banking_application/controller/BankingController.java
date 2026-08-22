package com.example.banking_application.controller;

import com.example.banking_application.dto.AccountRequest;
import com.example.banking_application.dto.AccountResponse;
import com.example.banking_application.model.Account;
import com.example.banking_application.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class BankingController {

    private final AccountService accountService;

    public BankingController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/accounts")
    public List<Account> getAllAccounts() {
        return accountService.getAllAccounts();
    }

    @GetMapping("/accounts/{id}")
    public Account getAccountById(@PathVariable Long id) {
        return accountService.getAccountById(id);
    }

    @PostMapping("/accounts")
    public AccountResponse createAccount(@Valid @RequestBody AccountRequest request) {
        return accountService.createAccount(request);
    }

    @PutMapping("/accounts/{id}")
    public AccountResponse updateAccount(@PathVariable Long id, @Valid @RequestBody AccountRequest request) {
        return accountService.updateAccount(id, request);
    }

    @DeleteMapping("/accounts/{id}")
    public String deleteAccount(@PathVariable Long id){
        accountService.deleteAccount(id);

        return "Account with ID " + id + " deleted successfully";
    }
}
