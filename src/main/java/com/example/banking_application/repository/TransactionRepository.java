package com.example.banking_application.repository;

import com.example.banking_application.model.Transaction;
import com.example.banking_application.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Page<Transaction> findByAccountId(
            Long accountId,
            Pageable pageable
    );

    Page<Transaction> findByAccountIdAndTransactionType(
            Long accountId,
            TransactionType transactionType,
            Pageable pageable
    );
}