package com.example.banking_application.service;

import com.example.banking_application.dto.UserRequest;
import com.example.banking_application.dto.UserResponse;
import com.example.banking_application.exception.DuplicateEmailException;
import com.example.banking_application.model.Role;
import com.example.banking_application.model.User;
import com.example.banking_application.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_shouldCreateUserSuccessfully() {

        UserRequest request = new UserRequest();

        request.setName("Abhishek");
        request.setEmail("TEST@GMAIL.COM");
        request.setPassword("password123");

        when(userRepository.existsByEmailIgnoreCase(
                "test@gmail.com"
        )).thenReturn(false);

        when(passwordEncoder.encode("password123"))
                .thenReturn("encodedPassword");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {

                    User user = invocation.getArgument(0);

                    user.setId(1L);

                    return user;
                });

        UserResponse response =
                userService.createUser(request);

        assertNotNull(response);

        assertEquals(
                1L,
                response.getId()
        );

        assertEquals(
                "Abhishek",
                response.getName()
        );

        assertEquals(
                "test@gmail.com",
                response.getEmail()
        );

        verify(
                userRepository,
                times(1)
        ).existsByEmailIgnoreCase(
                "test@gmail.com"
        );

        verify(
                passwordEncoder,
                times(1)
        ).encode(
                "password123"
        );

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(
                userRepository,
                times(1)
        ).save(
                userCaptor.capture()
        );

        User savedUser =
                userCaptor.getValue();

        assertEquals(
                "encodedPassword",
                savedUser.getPassword()
        );

        assertEquals(
                Role.USER,
                savedUser.getRole()
        );

        assertEquals(
                "test@gmail.com",
                savedUser.getEmail()
        );

        assertEquals(
                "Abhishek",
                savedUser.getName()
        );
    }

    @Test
    void createUser_shouldThrowExceptionWhenEmailAlreadyExists() {

        UserRequest request = new UserRequest();

        request.setName("Abhishek");
        request.setEmail("test@gmail.com");
        request.setPassword("password123");

        when(userRepository.existsByEmailIgnoreCase(
                "test@gmail.com"
        )).thenReturn(true);

        DuplicateEmailException exception =
                assertThrows(
                        DuplicateEmailException.class,
                        () -> userService.createUser(request)
                );

        assertEquals(
                "An account with this email already exists",
                exception.getMessage()
        );

        verify(
                userRepository,
                times(1)
        ).existsByEmailIgnoreCase(
                "test@gmail.com"
        );

        verify(
                userRepository,
                never()
        ).save(
                any(User.class)
        );

        verify(
                passwordEncoder,
                never()
        ).encode(
                anyString()
        );
    }
}