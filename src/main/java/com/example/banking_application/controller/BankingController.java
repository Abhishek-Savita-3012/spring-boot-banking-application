package com.example.banking_application.controller;

import com.example.banking_application.dto.*;
import com.example.banking_application.model.Account;
import com.example.banking_application.model.TransactionType;
import com.example.banking_application.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
public class BankingController {

    private final AccountService accountService;

    public BankingController(AccountService accountService) {
        this.accountService = accountService;
    }

//    @GetMapping("/accounts")
//    public List<AccountResponse> getAllAccounts() {
//        return accountService.getAllAccounts();
//    }

    @GetMapping("/accounts")
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {

        List<AccountResponse> accounts = accountService.getAllAccounts();

        return ResponseEntity.ok(accounts);
    }

//    @GetMapping("/accounts/{id}")
//    public Account getAccountById(@PathVariable Long id) {
//        return accountService.getAccountById(id);
//    }

    @GetMapping("/accounts/{id}")
    public ResponseEntity<AccountResponse> getAccountById(@PathVariable Long id) {

        AccountResponse response = accountService.getAccountById(id);

        return ResponseEntity.ok(response);
    }

//    @PostMapping("/accounts")
//    public AccountResponse createAccount(@Valid @RequestBody AccountRequest request) {
//        return accountService.createAccount(request);
//    }

    @PostMapping("/accounts")
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody AccountRequest request) {

        AccountResponse response = accountService.createAccount(request);

        URI location = URI.create("/api/accounts/" + response.getId());

        return ResponseEntity
                .created(location)
                .body(response);
    }

//    @PutMapping("/accounts/{id}")
//    public AccountResponse updateAccount(@PathVariable Long id, @Valid @RequestBody AccountRequest request) {
//        return accountService.updateAccount(id, request);
//    }

    @PutMapping("/accounts/{id}")
    public ResponseEntity<AccountResponse> updateAccount(@PathVariable Long id, @Valid @RequestBody AccountRequest request) {

        AccountResponse response =
                accountService.updateAccount(id, request);

        return ResponseEntity.ok(response);
    }

//    @DeleteMapping("/accounts/{id}")
//    public String deleteAccount(@PathVariable Long id){
//        accountService.deleteAccount(id);
//
//        return "Account with ID " + id + " deleted successfully";
//    }

    @DeleteMapping("/accounts/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {

        accountService.deleteAccount(id);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/accounts/{id}/deposit")
    public ResponseEntity<AccountResponse> deposit(@PathVariable Long id, @Valid @RequestBody TransactionRequest request) {
        AccountResponse response = accountService.deposit(id, request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/accounts/{id}/withdraw")
    public ResponseEntity<AccountResponse> withdraw(@PathVariable Long id, @Valid @RequestBody TransactionRequest request) {

        AccountResponse response = accountService.withdraw(id, request);

        return ResponseEntity.ok(response);
    }

//    @GetMapping("/accounts/{id}/transactions")
//    public List<TransactionResponse> getTransactionHistory(
//            @PathVariable Long id,
//            @RequestParam(required = false) TransactionType type) {
//
//        return accountService.getTransactionHistory(id, type);
//    }

//    @GetMapping("/accounts/{id}/transactions")
//    public TransactionPageResponse getTransactionHistory(
//            @PathVariable Long id,
//            @RequestParam(required = false) TransactionType type,
//            @PageableDefault(
//                    size = 10,
//                    sort = "transactionDate",
//                    direction = Sort.Direction.DESC
//            )
//            Pageable pageable) {
//
//        return accountService.getTransactionHistory(
//                id,
//                type,
//                pageable
//        );
//    }

    @PostMapping("/accounts/{id}/transfer")
    public ResponseEntity<String> transfer(@PathVariable Long id, @Valid @RequestBody TransferRequest request) {
        accountService.transfer(id, request);
        return ResponseEntity.ok("Transfer completed successfully");
    }

    @GetMapping("/accounts/{id}/transactions")
    public ResponseEntity<TransactionPageResponse> getTransactionHistory(
            @PathVariable Long id,
            @RequestParam(required = false)
            TransactionType type,
            @PageableDefault(
                    size = 10,
                    sort = "transactionDate",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable) {

        TransactionPageResponse response =
                accountService.getTransactionHistory(
                        id,
                        type,
                        pageable
                );

        return ResponseEntity.ok(response);
    }
}
