package com.example.banking_application.dto;

import com.example.banking_application.model.AccountType;
import jakarta.validation.constraints.NotNull;

public class AccountUpdateRequest {

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }
}
