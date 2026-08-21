package com.example.banking_application.controller;

import com.example.banking_application.service.BankingService;
import com.example.banking_application.service.CustomerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BankingController {

    private final BankingService bankingService;
    private final CustomerService customerService;

    public BankingController(BankingService bankingService, CustomerService customerService) {
        this.bankingService = bankingService;
        this.customerService = customerService;
    }

    @GetMapping("/hello")
    public String hello(){
        return "Welcome to Banking Application";
    }

    @GetMapping("/bank/status")
    public String bankStatus(){
        return bankingService.getBankStatus();
    }

    @GetMapping("/accounts")
    public String accounts(){
        return bankingService.getAccountMessage();
    }

    @GetMapping("/customers")
    public String customers(){
        return customerService.getCustomerMessage();
    }
}
