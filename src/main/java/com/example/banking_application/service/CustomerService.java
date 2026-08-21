package com.example.banking_application.service;

import org.springframework.stereotype.Service;

@Service
public class CustomerService {
    public String getCustomerMessage(){
        return "Customer service is ready";
    }
}
