package com.example.banking_application.controller;

import com.example.banking_application.dto.*;
import com.example.banking_application.model.Account;
import com.example.banking_application.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
    public List<AccountResponse> getAllAccounts() {
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

    @PostMapping("/accounts/{id}/deposit")
    public AccountResponse deposit(@PathVariable Long id, @Valid @RequestBody TransactionRequest request) {
        return accountService.deposit(id, request);
    }

    @PostMapping("/accounts/{id}/withdraw")
    public AccountResponse withdraw(@PathVariable Long id, @Valid @RequestBody TransactionRequest request) {

        return accountService.withdraw(id, request);
    }

    @GetMapping("/accounts/{id}/transactions")
    public List<TransactionResponse> getTransactionHistory(@PathVariable Long id) {
        return accountService.getTransactionHistory(id);
    }

    @PostMapping("/accounts/{id}/transfer")
    public ResponseEntity<String> transfer(@PathVariable Long id, @Valid @RequestBody TransferRequest request) {
        accountService.transfer(id, request);
        return ResponseEntity.ok("Transfer completed successfully");
    }
}
