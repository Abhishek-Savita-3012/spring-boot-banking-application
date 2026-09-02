package com.example.banking_application.integration;

import com.example.banking_application.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() throws Exception {

        userRepository.deleteAll();

        String registrationRequest = """
                {
                    "name": "Abhishek",
                    "email": "test@gmail.com",
                    "password": "password123"
                }
                """;

        mockMvc.perform(
                        post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registrationRequest)
                )
                .andExpect(status().isOk());

        assertEquals(
                1,
                userRepository.count()
        );
    }

    @Test
    void login_shouldLoginSuccessfullyWithValidCredentials()
            throws Exception {

        String requestBody = """
                {
                    "email": "test@gmail.com",
                    "password": "password123"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.userId").exists()
                )
                .andExpect(
                        jsonPath("$.name")
                                .value("Abhishek")
                )
                .andExpect(
                        jsonPath("$.email")
                                .value("test@gmail.com")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("Login successful")
                )
                .andExpect(
                        jsonPath("$.token")
                                .value(
                                        not(
                                                emptyOrNullString()
                                        )
                                )
                );
    }

    @Test
    void login_shouldNormalizeUppercaseEmail()
            throws Exception {

        String requestBody = """
                {
                    "email": "TEST@GMAIL.COM",
                    "password": "password123"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.email")
                                .value("test@gmail.com")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("Login successful")
                )
                .andExpect(
                        jsonPath("$.token")
                                .value(
                                        not(
                                                emptyOrNullString()
                                        )
                                )
                );
    }

    @Test
    void login_shouldReturnUnauthorizedWhenPasswordIsIncorrect()
            throws Exception {

        String requestBody = """
                {
                    "email": "test@gmail.com",
                    "password": "wrongPassword"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.status")
                                .value(401)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Invalid Credentials")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Invalid email or password"
                                )
                );
    }

    @Test
    void login_shouldReturnUnauthorizedWhenUserDoesNotExist()
            throws Exception {

        String requestBody = """
                {
                    "email": "missing@gmail.com",
                    "password": "password123"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.status")
                                .value(401)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Invalid Credentials")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Invalid email or password"
                                )
                );
    }

    @Test
    void login_shouldReturnBadRequestWhenEmailIsInvalid()
            throws Exception {

        String requestBody = """
                {
                    "email": "not-an-email",
                    "password": "password123"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Validation Error")
                );
    }
}