package com.example.banking_application.controller;

import com.example.banking_application.dto.*;
import com.example.banking_application.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

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

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/accounts")
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {

        List<AccountResponse> accounts = accountService.getAllAccounts();

        return ResponseEntity.ok(accounts);
    }

//    @GetMapping("/accounts/{id}")
//    public Account getAccountById(@PathVariable Long id) {
//        return accountService.getAccountById(id);
//    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/accounts/{id}")
    public ResponseEntity<AccountResponse> getAccountById(@PathVariable Long id) {

        AccountResponse response = accountService.getAccountById(id);

        return ResponseEntity.ok(response);
    }

//    @PostMapping("/accounts")
//    public AccountResponse createAccount(@Valid @RequestBody AccountRequest request) {
//        return accountService.createAccount(request);
//    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
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

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/accounts/{id}")
    public ResponseEntity<AccountResponse> updateAccount(@PathVariable Long id, @Valid @RequestBody AccountUpdateRequest request) {

        AccountResponse response = accountService.updateAccount(id, request);

        return ResponseEntity.ok(response);
    }

//    @DeleteMapping("/accounts/{id}")
//    public String deleteAccount(@PathVariable Long id){
//        accountService.deleteAccount(id);
//
//        return "Account with ID " + id + " deleted successfully";
//    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/accounts/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {

        accountService.deleteAccount(id);

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/accounts/{id}/status")
    public ResponseEntity<AccountResponse> updateAccountStatus(@PathVariable Long id, @Valid @RequestBody AccountStatusRequest request) {

        AccountResponse response = accountService.updateAccountStatus(id, request);

        return ResponseEntity.ok(response);
    }
}
