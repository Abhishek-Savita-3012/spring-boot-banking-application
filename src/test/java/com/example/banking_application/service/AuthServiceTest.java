package com.example.banking_application.service;

import com.example.banking_application.dto.LoginRequest;
import com.example.banking_application.dto.LoginResponse;
import com.example.banking_application.exception.InvalidCredentialsException;
import com.example.banking_application.model.Role;
import com.example.banking_application.model.User;
import com.example.banking_application.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_shouldReturnLoginResponseWhenCredentialsAreValid() {

        LoginRequest request = new LoginRequest();

        request.setEmail("TEST@GMAIL.COM");
        request.setPassword("password123");

        User user = new User();

        user.setId(1L);
        user.setName("Abhishek");
        user.setEmail("test@gmail.com");
        user.setPassword("encodedPassword");
        user.setRole(Role.USER);

        when(userRepository.findByEmailIgnoreCase(
                "test@gmail.com"
        )).thenReturn(
                Optional.of(user)
        );

        when(passwordEncoder.matches(
                "password123",
                "encodedPassword"
        )).thenReturn(true);

        when(jwtService.generateToken(
                "test@gmail.com",
                "USER"
        )).thenReturn(
                "test-jwt-token"
        );

        LoginResponse response =
                authService.login(request);

        assertNotNull(response);

        assertEquals(
                1L,
                response.getUserId()
        );

        assertEquals(
                "Abhishek",
                response.getName()
        );

        assertEquals(
                "test@gmail.com",
                response.getEmail()
        );

        assertEquals(
                "Login successful",
                response.getMessage()
        );

        assertEquals(
                "test-jwt-token",
                response.getToken()
        );

        verify(
                userRepository,
                times(1)
        ).findByEmailIgnoreCase(
                "test@gmail.com"
        );

        verify(
                passwordEncoder,
                times(1)
        ).matches(
                "password123",
                "encodedPassword"
        );

        verify(
                jwtService,
                times(1)
        ).generateToken(
                "test@gmail.com",
                "USER"
        );
    }

    @Test
    void login_shouldThrowInvalidCredentialsWhenUserDoesNotExist() {

        LoginRequest request = new LoginRequest();

        request.setEmail("MISSING@GMAIL.COM");
        request.setPassword("password123");

        when(userRepository.findByEmailIgnoreCase(
                "missing@gmail.com"
        )).thenReturn(
                Optional.empty()
        );

        InvalidCredentialsException exception =
                assertThrows(
                        InvalidCredentialsException.class,
                        () -> authService.login(request)
                );

        assertEquals(
                "Invalid email or password",
                exception.getMessage()
        );

        verify(
                userRepository,
                times(1)
        ).findByEmailIgnoreCase(
                "missing@gmail.com"
        );

        verify(
                passwordEncoder,
                never()
        ).matches(
                anyString(),
                anyString()
        );

        verify(
                jwtService,
                never()
        ).generateToken(
                anyString(),
                anyString()
        );
    }

    @Test
    void login_shouldThrowInvalidCredentialsWhenPasswordIsIncorrect() {

        LoginRequest request = new LoginRequest();

        request.setEmail("test@gmail.com");
        request.setPassword("wrongPassword");

        User user = new User();

        user.setId(1L);
        user.setName("Abhishek");
        user.setEmail("test@gmail.com");
        user.setPassword("encodedPassword");
        user.setRole(Role.USER);

        when(userRepository.findByEmailIgnoreCase(
                "test@gmail.com"
        )).thenReturn(
                Optional.of(user)
        );

        when(passwordEncoder.matches(
                "wrongPassword",
                "encodedPassword"
        )).thenReturn(false);

        InvalidCredentialsException exception =
                assertThrows(
                        InvalidCredentialsException.class,
                        () -> authService.login(request)
                );

        assertEquals(
                "Invalid email or password",
                exception.getMessage()
        );

        verify(
                userRepository,
                times(1)
        ).findByEmailIgnoreCase(
                "test@gmail.com"
        );

        verify(
                passwordEncoder,
                times(1)
        ).matches(
                "wrongPassword",
                "encodedPassword"
        );

        verify(
                jwtService,
                never()
        ).generateToken(
                anyString(),
                anyString()
        );
    }
}