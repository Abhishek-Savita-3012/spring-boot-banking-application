package com.example.banking_application.dto;

import com.example.banking_application.model.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public class AccountRequest {

    @Schema(
            description = "Unique bank account number",
            example = "ACC100001"
    )
    @NotBlank(message = "Account number is required")
    @Size(min = 6, max = 20, message = "Account number must be between 6 and 20 characters")
    private String accountNumber;

    @Schema(
            description = "Type of bank account",
            example = "SAVINGS",
            allowableValues = {"SAVINGS", "CURRENT"}
    )
    @NotNull(message = "Account type is required")
    private AccountType accountType;

    public AccountRequest() {
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }
}