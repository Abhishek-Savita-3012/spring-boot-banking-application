package com.example.banking_application.service;

import org.springframework.stereotype.Service;

@Service
public class BankingService {
    public String getBankStatus(){
        return "Banking Application is running";
    }

    public String getAccountMessage(){
        return "Account service is ready";
    }
}
