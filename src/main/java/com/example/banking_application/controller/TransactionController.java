package com.example.banking_application.controller;

import com.example.banking_application.dto.AccountResponse;
import com.example.banking_application.dto.TransactionPageResponse;
import com.example.banking_application.dto.TransactionRequest;
import com.example.banking_application.dto.TransferRequest;
import com.example.banking_application.model.TransactionType;
import com.example.banking_application.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api")
public class TransactionController {

    private final TransactionService transactionService;
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/transactions/{id}/deposit")
    public ResponseEntity<AccountResponse> deposit(@PathVariable Long id, @Valid @RequestBody TransactionRequest request) {
        AccountResponse response = transactionService.deposit(id, request);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/transactions/{id}/withdraw")
    public ResponseEntity<AccountResponse> withdraw(@PathVariable Long id, @Valid @RequestBody TransactionRequest request) {

        AccountResponse response = transactionService.withdraw(id, request);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/transactions/{id}/transfer")
    public ResponseEntity<String> transfer(@PathVariable Long id, @Valid @RequestBody TransferRequest request) {
        transactionService.transfer(id, request);
        return ResponseEntity.ok("Transfer completed successfully");
    }

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
            Pageable pageable) {

        TransactionPageResponse response =
                transactionService.getTransactionHistory(
                        id,
                        type,
                        pageable
                );

        return ResponseEntity.ok(response);
    }
}
