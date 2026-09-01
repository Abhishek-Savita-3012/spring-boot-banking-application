package com.example.banking_application.dto;

import com.example.banking_application.model.AccountStatus;
import jakarta.validation.constraints.NotNull;

public class AccountStatusRequest {

    @NotNull(message = "Account status is required")
    private AccountStatus status;

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }
}