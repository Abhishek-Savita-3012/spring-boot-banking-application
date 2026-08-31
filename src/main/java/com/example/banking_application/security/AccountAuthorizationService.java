package com.example.banking_application.security;

import com.example.banking_application.model.Account;
import com.example.banking_application.model.Role;
import com.example.banking_application.model.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AccountAuthorizationService {

    public User getCurrentUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof User)) {

            throw new AccessDeniedException(
                    "Authenticated user not found"
            );
        }

        return (User) authentication.getPrincipal();
    }

    public boolean isAdmin() {

        User currentUser = getCurrentUser();

        return currentUser.getRole() == Role.ADMIN;
    }

    public void checkAccountOwnership(Account account) {

        User currentUser = getCurrentUser();

        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }

        if (!account.getUser()
                .getId()
                .equals(currentUser.getId())) {

            throw new AccessDeniedException(
                    "You are not authorized to access this account"
            );
        }
    }
}