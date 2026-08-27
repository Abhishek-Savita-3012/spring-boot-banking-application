package com.example.banking_application.controller;

import com.example.banking_application.model.User;
import com.example.banking_application.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {

        return ResponseEntity.ok(
                userService.createUser(user)
        );
    }
}