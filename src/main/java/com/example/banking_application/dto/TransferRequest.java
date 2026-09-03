package com.example.banking_application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public class TransferRequest {

    @Schema(
            description = "ID of the destination account",
            example = "2"
    )
    @NotNull(message = "Receiver account ID is required")
    private Long receiverAccountId;

    @Schema(
            description = "Amount to transfer. Must be greater than zero.",
            example = "250.00"
    )
    @NotNull(message = "Transfer amount is required")
    @DecimalMin(value = "0.01", message = "Transfer amount must be greater than 0")
    private BigDecimal amount;

    public TransferRequest() {
    }

    public Long getReceiverAccountId() {
        return receiverAccountId;
    }

    public void setReceiverAccountId(Long receiverAccountId) {
        this.receiverAccountId = receiverAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}