package com.example.banking_application.integration;

import com.example.banking_application.model.User;
import com.example.banking_application.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.example.banking_application.repository.AccountRepository;
import com.example.banking_application.repository.TransactionRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {

        // Delete child records before parent records
        // to respect foreign-key relationships.
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createUser_shouldCreateUserSuccessfully() throws Exception {

        String requestBody = """
                {
                    "name": "Abhishek",
                    "email": "TEST@GMAIL.COM",
                    "password": "password123"
                }
                """;

        mockMvc.perform(
                        post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Abhishek"))
                .andExpect(jsonPath("$.email").value("test@gmail.com"));

        assertEquals(
                1,
                userRepository.count()
        );

        User savedUser =
                userRepository.findByEmailIgnoreCase(
                        "test@gmail.com"
                ).orElseThrow();

        assertEquals(
                "Abhishek",
                savedUser.getName()
        );

        assertEquals(
                "test@gmail.com",
                savedUser.getEmail()
        );

        assertNotEquals(
                "password123",
                savedUser.getPassword()
        );

        assertTrue(
                savedUser.getPassword()
                        .startsWith("$2")
        );
    }

    @Test
    void createUser_shouldReturnConflictWhenEmailAlreadyExists()
            throws Exception {

        String requestBody = """
                {
                    "name": "Abhishek",
                    "email": "test@gmail.com",
                    "password": "password123"
                }
                """;

        mockMvc.perform(
                        post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk());

        String duplicateRequest = """
                {
                    "name": "Another User",
                    "email": "TEST@GMAIL.COM",
                    "password": "anotherPassword123"
                }
                """;

        mockMvc.perform(
                        post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(duplicateRequest)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "An account with this email already exists"
                                )
                );

        assertEquals(
                1,
                userRepository.count()
        );
    }

    @Test
    void createUser_shouldReturnBadRequestWhenEmailIsInvalid()
            throws Exception {

        String requestBody = """
                {
                    "name": "Abhishek",
                    "email": "not-an-email",
                    "password": "password123"
                }
                """;

        mockMvc.perform(
                        post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());

        assertEquals(
                0,
                userRepository.count()
        );
    }

    @Test
    void createUser_shouldReturnBadRequestWhenPasswordIsTooShort()
            throws Exception {

        String requestBody = """
                {
                    "name": "Abhishek",
                    "email": "test@gmail.com",
                    "password": "123"
                }
                """;

        mockMvc.perform(
                        post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());

        assertEquals(
                0,
                userRepository.count()
        );
    }
}