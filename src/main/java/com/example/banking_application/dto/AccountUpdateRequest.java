package com.example.banking_application.dto;

import com.example.banking_application.model.AccountType;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

public class AccountUpdateRequest {

    @Schema(
            description = "New account type",
            example = "CURRENT",
            allowableValues = {"SAVINGS", "CURRENT"}
    )
    @NotNull(message = "Account type is required")
    private AccountType accountType;

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }
}
