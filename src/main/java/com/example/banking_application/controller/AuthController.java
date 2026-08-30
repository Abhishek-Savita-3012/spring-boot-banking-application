package com.example.banking_application.controller;

import com.example.banking_application.dto.LoginRequest;
import com.example.banking_application.dto.LoginResponse;
import com.example.banking_application.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {

        LoginResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/test")
    public ResponseEntity<String> adminTest() {

        return ResponseEntity.ok(
                "Welcome Admin"
        );
    }
}