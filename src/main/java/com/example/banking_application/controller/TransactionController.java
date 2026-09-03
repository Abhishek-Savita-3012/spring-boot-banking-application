package com.example.banking_application.controller;

import com.example.banking_application.dto.AccountResponse;
import com.example.banking_application.dto.TransactionPageResponse;
import com.example.banking_application.dto.TransactionRequest;
import com.example.banking_application.dto.TransferRequest;
import com.example.banking_application.model.TransactionType;
import com.example.banking_application.service.TransactionService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

@Tag(
        name = "Transactions",
        description = "APIs for deposits, withdrawals, transfers, and transaction history"
)
@RestController
@RequestMapping("/api")
public class TransactionController {

    private final TransactionService transactionService;
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Operation(
            summary = "Deposit money",
            description = "Deposits money into an account owned by the authenticated USER. The account must be ACTIVE."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid transaction request"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Account access denied"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "409", description = "Account state does not allow this transaction")
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/transactions/{id}/deposit")
    public ResponseEntity<AccountResponse> deposit(@PathVariable Long id, @Valid @RequestBody TransactionRequest request) {
        AccountResponse response = transactionService.deposit(id, request);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Withdraw money",
            description = "Withdraws money from an account owned by the authenticated USER. The account must be ACTIVE and have sufficient balance."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid transaction request"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Account access denied"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "409", description = "Account state does not allow this transaction")
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/transactions/{id}/withdraw")
    public ResponseEntity<AccountResponse> withdraw(@PathVariable Long id, @Valid @RequestBody TransactionRequest request) {

        AccountResponse response = transactionService.withdraw(id, request);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Transfer money",
            description = "Transfers money from the authenticated USER's account to another account. Both accounts must be ACTIVE and the sender must have sufficient balance."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid transaction request"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Account access denied"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "409", description = "Account state does not allow this transaction")
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/transactions/{id}/transfer")
    public ResponseEntity<String> transfer(@PathVariable Long id, @Valid @RequestBody TransferRequest request) {
        transactionService.transfer(id, request);
        return ResponseEntity.ok("Transfer completed successfully");
    }

    @Operation(
            summary = "Get transaction history",
            description = "Returns paginated transaction history for an account owned by the authenticated USER. Results can optionally be filtered by transaction type."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction history returned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination or transaction type"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Account access denied"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/transactions/{id}/records")
    public ResponseEntity<TransactionPageResponse> getTransactionHistory(
            @PathVariable Long id,
            @RequestParam(required = false)
            TransactionType type,
            @PageableDefault(
                    size = 10,
                    sort = "transactionDate",
                    direction = Sort.Direction.DESC
            )
            @ParameterObject Pageable pageable) {

        TransactionPageResponse response =
                transactionService.getTransactionHistory(
                        id,
                        type,
                        pageable
                );

        return ResponseEntity.ok(response);
    }
}
