package com.example.banking_application.controller;

import com.example.banking_application.dto.*;
import com.example.banking_application.service.AccountService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.net.URI;
import java.util.List;

@Tag(
        name = "Accounts",
        description = "APIs for creating, viewing, updating, and managing bank accounts"
)
@RestController
@RequestMapping("/api")
public class BankingController {

    private final AccountService accountService;

    public BankingController(AccountService accountService) {
        this.accountService = accountService;
    }

    @Operation(
            summary = "Get all accounts",
            description = "Returns all bank accounts. This operation is restricted to ADMIN users."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Accounts returned successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "ADMIN role required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/accounts")
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {

        List<AccountResponse> accounts = accountService.getAllAccounts();

        return ResponseEntity.ok(accounts);
    }

    @Operation(
            summary = "Get account by ID",
            description = "Returns an account by ID. USER can access only their own account, while ADMIN can access any account."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account returned successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/accounts/{id}")
    public ResponseEntity<AccountResponse> getAccountById(@PathVariable Long id) {

        AccountResponse response = accountService.getAccountById(id);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Create bank account",
            description = "Creates a new bank account for the currently authenticated user with zero balance and ACTIVE status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid account data"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "409", description = "Account number already exists")
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/accounts")
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody AccountRequest request) {

        AccountResponse response = accountService.createAccount(request);

        URI location = URI.create("/api/accounts/" + response.getId());

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @Operation(
            summary = "Update account type",
            description = "Updates the account type of an existing bank account. This operation is restricted to ADMIN users."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account type updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid account data"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "ADMIN role required"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/accounts/{id}")
    public ResponseEntity<AccountResponse> updateAccount(@PathVariable Long id, @Valid @RequestBody AccountUpdateRequest request) {

        AccountResponse response = accountService.updateAccount(id, request);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Update account status",
            description = "Updates an account status. This operation is restricted to ADMIN users. Closing an account requires a zero balance, and a CLOSED account cannot be reopened."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "ADMIN role required"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "409", description = "Invalid account state transition")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/accounts/{id}/status")
    public ResponseEntity<AccountResponse> updateAccountStatus(@PathVariable Long id, @Valid @RequestBody AccountStatusRequest request) {

        AccountResponse response = accountService.updateAccountStatus(id, request);

        return ResponseEntity.ok(response);
    }
}
