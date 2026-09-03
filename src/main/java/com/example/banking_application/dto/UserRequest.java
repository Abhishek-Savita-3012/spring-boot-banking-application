package com.example.banking_application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public class UserRequest {

    @Schema(
            description = "Full name of the user",
            example = "Abhishek Savita"
    )
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Schema(
            description = "Unique email address used for login",
            example = "abhishek@example.com"
    )
    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email")
    @Size(max = 254, message = "Email is too long")
    private String email;

    @Schema(
            description = "Password with at least 8 characters",
            example = "SecurePass123"
    )
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;

    public UserRequest() {
    }

    public UserRequest(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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