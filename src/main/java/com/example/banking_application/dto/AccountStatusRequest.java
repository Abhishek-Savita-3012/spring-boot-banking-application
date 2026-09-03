package com.example.banking_application.dto;

import com.example.banking_application.model.AccountStatus;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

public class AccountStatusRequest {

    @Schema(
            description = "New lifecycle status of the account",
            example = "BLOCKED",
            allowableValues = {"ACTIVE", "BLOCKED", "CLOSED"}
    )
    @NotNull(message = "Account status is required")
    private AccountStatus status;

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }
}