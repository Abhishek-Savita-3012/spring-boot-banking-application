package com.example.banking_application.service;

import com.example.banking_application.dto.UserRequest;
import com.example.banking_application.dto.UserResponse;
import com.example.banking_application.model.Role;
import com.example.banking_application.model.User;
import com.example.banking_application.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.example.banking_application.exception.DuplicateEmailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private UserResponse convertToResponse(User user) {

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }

    public UserResponse createUser(UserRequest request) {

        String normalizedEmail =
                request.getEmail()
                        .trim()
                        .toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateEmailException("An account with this email already exists");
        }

        User user = new User();

        user.setName(request.getName().trim());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);

        User savedUser = userRepository.save(user);

        logger.info("User registration completed userId={}", savedUser.getId());

        return convertToResponse(savedUser);
    }
}