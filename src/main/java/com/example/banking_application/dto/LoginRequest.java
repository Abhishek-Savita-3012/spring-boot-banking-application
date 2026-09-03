package com.example.banking_application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

public class LoginRequest {

    @Schema(
            description = "Registered email address",
            example = "abhishek@example.com"
    )
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email")
    private String email;

    @Schema(
            description = "User password",
            example = "SecurePass123"
    )
    @NotBlank(message = "Password is required")
    private String password;

    public LoginRequest() {
    }

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}