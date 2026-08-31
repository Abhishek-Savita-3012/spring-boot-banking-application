package com.example.banking_application.service;

import com.example.banking_application.dto.*;
import com.example.banking_application.exception.*;
import com.example.banking_application.model.Account;
import com.example.banking_application.model.Role;
import com.example.banking_application.model.User;
import com.example.banking_application.repository.AccountRepository;
import com.example.banking_application.repository.UserRepository;
import org.springframework.stereotype.Service;
import com.example.banking_application.security.AccountAuthorizationService;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AccountAuthorizationService accountAuthorizationService;

    public AccountService(AccountRepository accountRepository, UserRepository userRepository, AccountAuthorizationService accountAuthorizationService) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.accountAuthorizationService = accountAuthorizationService;
    }

    private AccountResponse convertToResponse(Account account) {

        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getBalance(),
                account.getUser().getId()
        );
    }

    public AccountResponse createAccount(AccountRequest request) {

        User currentUser = accountAuthorizationService.getCurrentUser();

        Long ownerId;

        if (currentUser.getRole() == Role.ADMIN) {

            ownerId = request.getUserId();

        } else {

            if (request.getUserId() != null && !request.getUserId().equals(currentUser.getId())) {

                throw new AccessDeniedException(
                        "You cannot create an account for another user"
                );
            }

            ownerId = currentUser.getId();
        }

        User user = userRepository.findById(ownerId)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found with id: "
                                        + ownerId
                        )
                );

        if (accountRepository.existsByAccountNumber(request.getAccountNumber())) {

            throw new DuplicateAccountException(
                    "Account number already exists: "
                            + request.getAccountNumber()
            );
        }

        Account account = new Account();

        account.setAccountNumber(request.getAccountNumber());
        account.setAccountType(request.getAccountType());
        account.setBalance(request.getBalance());
        account.setUser(user);

        Account savedAccount = accountRepository.save(account);

        return convertToResponse(savedAccount);
    }

    public List<AccountResponse> getAllAccounts() {

        return accountRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    public AccountResponse getAccountById(Long id) {

        Account account = accountRepository.findById(id)
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Account with ID " + id + " not found"
                        )
                );

        accountAuthorizationService.checkAccountOwnership(account);

        return convertToResponse(account);
    }

    public AccountResponse updateAccount(Long id, AccountRequest request) {

        Account existingAccount = accountRepository.findById(id)
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Account with ID " + id + " not found"
                        )
                );

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User with ID " + request.getUserId() + " not found"
                        )
                );

        existingAccount.setAccountNumber(
                request.getAccountNumber()
        );

        existingAccount.setAccountType(
                request.getAccountType()
        );

        existingAccount.setBalance(
                request.getBalance()
        );

        existingAccount.setUser(user);

        Account savedAccount = accountRepository.save(existingAccount);

        return convertToResponse(savedAccount);
    }

    public void deleteAccount(Long id) {

        Account account = accountRepository.findById(id)
                .orElseThrow(() ->
                        new AccountNotFoundException("Account with ID " + id + " not found")
                );

        accountRepository.delete(account);
    }
}
