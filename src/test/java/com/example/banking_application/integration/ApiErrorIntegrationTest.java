package com.example.banking_application.integration;

import com.example.banking_application.model.Role;
import com.example.banking_application.model.User;
import com.example.banking_application.repository.AccountRepository;
import com.example.banking_application.repository.TransactionRepository;
import com.example.banking_application.repository.UserRepository;
import com.example.banking_application.service.JwtService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiErrorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private JwtService jwtService;

    private User normalUser;
    private User adminUser;

    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {

        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        registerUser(
                "Normal User",
                "user@gmail.com"
        );

        registerUser(
                "Admin User",
                "admin@gmail.com"
        );

        normalUser = userRepository
                .findByEmailIgnoreCase("user@gmail.com")
                .orElseThrow();

        adminUser = userRepository
                .findByEmailIgnoreCase("admin@gmail.com")
                .orElseThrow();

        adminUser.setRole(Role.ADMIN);
        adminUser = userRepository.save(adminUser);

        userToken = jwtService.generateToken(
                normalUser.getEmail(),
                normalUser.getRole().toString()
        );

        adminToken = jwtService.generateToken(
                adminUser.getEmail(),
                adminUser.getRole().toString()
        );
    }

    // =========================================================
    // Malformed JSON
    // =========================================================

    @Test
    void createAccount_shouldReturnBadRequestForMalformedJson()
            throws Exception {

        String malformedJson = """
                {
                    "accountNumber": "ERR10001",
                    "accountType": "SAVINGS"
                """;

        mockMvc.perform(
                        post("/api/accounts")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(malformedJson)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Bad Request")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Invalid request body or enum value"
                                )
                );

        assertEquals(
                0,
                accountRepository.count()
        );
    }

    // =========================================================
    // Invalid Enum Values
    // =========================================================

    @Test
    void createAccount_shouldReturnBadRequestForInvalidAccountType()
            throws Exception {

        String requestBody = """
                {
                    "accountNumber": "ERR10002",
                    "accountType": "INVALID_TYPE"
                }
                """;

        mockMvc.perform(
                        post("/api/accounts")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
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
                                .value("Bad Request")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Invalid request body or enum value"
                                )
                );

        assertEquals(
                0,
                accountRepository.count()
        );
    }

    @Test
    void updateStatus_shouldReturnBadRequestForInvalidAccountStatus()
            throws Exception {

        Long accountId = createAccount(
                userToken,
                "ERR10003"
        );

        String requestBody = """
                {
                    "status": "FROZEN"
                }
                """;

        mockMvc.perform(
                        patch(
                                "/api/accounts/{id}/status",
                                accountId
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
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
                                .value("Bad Request")
                );
    }

    // =========================================================
    // Bean Validation
    // =========================================================

    @Test
    void createAccount_shouldReturnValidationErrorForMissingAccountNumber()
            throws Exception {

        String requestBody = """
                {
                    "accountType": "SAVINGS"
                }
                """;

        mockMvc.perform(
                        post("/api/accounts")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
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

        assertEquals(
                0,
                accountRepository.count()
        );
    }

    @Test
    void createAccount_shouldReturnValidationErrorForShortAccountNumber()
            throws Exception {

        String requestBody = """
                {
                    "accountNumber": "123",
                    "accountType": "SAVINGS"
                }
                """;

        mockMvc.perform(
                        post("/api/accounts")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
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

        assertEquals(
                0,
                accountRepository.count()
        );
    }

    @Test
    void deposit_shouldReturnValidationErrorForMissingAmount()
            throws Exception {

        Long accountId = createAccount(
                userToken,
                "ERR10004"
        );

        String requestBody = """
                {
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/transactions/{id}/deposit",
                                accountId
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
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

        assertEquals(
                0,
                transactionRepository.count()
        );
    }

    @Test
    void transfer_shouldReturnValidationErrorForMissingReceiverAccountId()
            throws Exception {

        Long senderId = createAccount(
                userToken,
                "ERR10005"
        );

        String requestBody = """
                {
                    "amount": 100.00
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/transactions/{id}/transfer",
                                senderId
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
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

        assertEquals(
                0,
                transactionRepository.count()
        );
    }

    // =========================================================
    // Invalid Query Parameters
    // =========================================================

    @Test
    void history_shouldReturnBadRequestForInvalidTransactionType()
            throws Exception {

        Long accountId = createAccount(
                userToken,
                "ERR10006"
        );

        mockMvc.perform(
                        get(
                                "/api/transactions/{id}/records",
                                accountId
                        )
                                .param(
                                        "type",
                                        "INVALID_TRANSACTION"
                                )
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Bad Request")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Invalid value for parameter: type"
                                )
                );
    }

    // =========================================================
    // Account Not Found
    // =========================================================

    @Test
    void getAccount_shouldReturnNotFoundForNonexistentAccount()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/accounts/{id}",
                                999999L
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Account Not Found")
                );
    }

    @Test
    void deposit_shouldReturnNotFoundForNonexistentAccount()
            throws Exception {

        String requestBody = """
                {
                    "amount": 100.00
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/transactions/{id}/deposit",
                                999999L
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Account Not Found")
                );

        assertEquals(
                0,
                transactionRepository.count()
        );
    }

    // =========================================================
    // Authentication
    // =========================================================

    @Test
    void protectedEndpoint_shouldReturnUnauthorizedWithoutJwt()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/accounts/{id}",
                                1L
                        )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.error")
                                .value("Unauthorized")
                );
    }

    @Test
    void protectedEndpoint_shouldReturnUnauthorizedForMalformedJwt()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/accounts/{id}",
                                1L
                        )
                                .header(
                                        "Authorization",
                                        "Bearer this.is.not.a.valid.jwt"
                                )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.error")
                                .value("Unauthorized")
                );
    }

    // =========================================================
    // Authorization
    // =========================================================

    @Test
    void normalUser_shouldReturnForbiddenForAdminOnlyEndpoint()
            throws Exception {

        mockMvc.perform(
                        get("/api/accounts")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
                )
                .andExpect(status().isForbidden());
    }

    // =========================================================
    // Helpers
    // =========================================================

    private void registerUser(
            String name,
            String email
    ) throws Exception {

        String requestBody = """
                {
                    "name": "%s",
                    "email": "%s",
                    "password": "password123"
                }
                """.formatted(name, email);

        mockMvc.perform(
                        post("/api/users")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isOk());
    }

    private Long createAccount(
            String token,
            String accountNumber
    ) throws Exception {

        String requestBody = """
                {
                    "accountNumber": "%s",
                    "accountType": "SAVINGS"
                }
                """.formatted(accountNumber);

        MvcResult result = mockMvc.perform(
                        post("/api/accounts")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andReturn();

        Integer accountId = JsonPath.read(
                result.getResponse()
                        .getContentAsString(),
                "$.id"
        );

        return accountId.longValue();
    }
}