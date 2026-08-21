package com.example.banking_application.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BankingController {

    @GetMapping("/hello")
    public String hello(){
        return "Welcome to Banking Application";
    }

    @GetMapping("/bank/status")
    public String status(){
        return "Banking Application is running";
    }
}
