package com.example.banking_application.exception;

public class DuplicateAccountException
        extends RuntimeException {

    public DuplicateAccountException(String message) {
        super(message);
    }
}
