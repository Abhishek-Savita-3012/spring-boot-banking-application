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
class AccountIntegrationTest {

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

    private User userOne;
    private User userTwo;
    private User adminUser;

    private String userOneToken;
    private String userTwoToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {

        // Delete child records before parent records
        // Transaction -> Account -> User
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        registerUser(
                "User One",
                "userone@gmail.com"
        );

        registerUser(
                "User Two",
                "usertwo@gmail.com"
        );

        registerUser(
                "Admin User",
                "admin@gmail.com"
        );

        userOne = userRepository
                .findByEmailIgnoreCase("userone@gmail.com")
                .orElseThrow();

        userTwo = userRepository
                .findByEmailIgnoreCase("usertwo@gmail.com")
                .orElseThrow();

        adminUser = userRepository
                .findByEmailIgnoreCase("admin@gmail.com")
                .orElseThrow();

        adminUser.setRole(Role.ADMIN);
        adminUser = userRepository.save(adminUser);

        userOneToken = jwtService.generateToken(
                userOne.getEmail(),
                userOne.getRole().toString()
        );

        userTwoToken = jwtService.generateToken(
                userTwo.getEmail(),
                userTwo.getRole().toString()
        );

        adminToken = jwtService.generateToken(
                adminUser.getEmail(),
                adminUser.getRole().toString()
        );
    }

    @Test
    void createAccount_shouldCreateAccountForAuthenticatedUser()
            throws Exception {

        String requestBody = """
                {
                    "accountNumber": "ACC10001",
                    "accountType": "SAVINGS"
                }
                """;

        mockMvc.perform(
                        post("/api/accounts")
                                .header(
                                        "Authorization",
                                        "Bearer " + userOneToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(
                        header().string(
                                "Location",
                                org.hamcrest.Matchers
                                        .startsWith("/api/accounts/")
                        )
                )
                .andExpect(
                        jsonPath("$.id").exists()
                )
                .andExpect(
                        jsonPath("$.accountNumber")
                                .value("ACC10001")
                )
                .andExpect(
                        jsonPath("$.accountType")
                                .value("SAVINGS")
                )
                .andExpect(
                        jsonPath("$.balance")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                )
                .andExpect(
                        jsonPath("$.userId")
                                .value(userOne.getId())
                );

        assertEquals(
                1,
                accountRepository.count()
        );
    }

    @Test
    void createAccount_shouldReturnConflictForDuplicateAccountNumber()
            throws Exception {

        createAccount(
                userOneToken,
                "ACC10002",
                "SAVINGS"
        );

        String duplicateRequest = """
                {
                    "accountNumber": "ACC10002",
                    "accountType": "CURRENT"
                }
                """;

        mockMvc.perform(
                        post("/api/accounts")
                                .header(
                                        "Authorization",
                                        "Bearer " + userTwoToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(duplicateRequest)
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.status")
                                .value(409)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "Account Number Already Exists"
                                )
                );

        assertEquals(
                1,
                accountRepository.count()
        );
    }

    @Test
    void createAccount_shouldReturnUnauthorizedWithoutToken()
            throws Exception {

        String requestBody = """
                {
                    "accountNumber": "ACC10003",
                    "accountType": "SAVINGS"
                }
                """;

        mockMvc.perform(
                        post("/api/accounts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.error")
                                .value("Unauthorized")
                );

        assertEquals(
                0,
                accountRepository.count()
        );
    }

    @Test
    void getAccountById_shouldAllowOwner()
            throws Exception {

        Long accountId = createAccount(
                userOneToken,
                "ACC10004",
                "CURRENT"
        );

        mockMvc.perform(
                        get("/api/accounts/{id}", accountId)
                                .header(
                                        "Authorization",
                                        "Bearer " + userOneToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(accountId)
                )
                .andExpect(
                        jsonPath("$.accountNumber")
                                .value("ACC10004")
                )
                .andExpect(
                        jsonPath("$.userId")
                                .value(userOne.getId())
                );
    }

    @Test
    void getAccountById_shouldDenyDifferentUser()
            throws Exception {

        Long accountId = createAccount(
                userOneToken,
                "ACC10005",
                "SAVINGS"
        );

        mockMvc.perform(
                        get("/api/accounts/{id}", accountId)
                                .header(
                                        "Authorization",
                                        "Bearer " + userTwoToken
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllAccounts_shouldReturnForbiddenForNormalUser()
            throws Exception {

        createAccount(
                userOneToken,
                "ACC10006",
                "SAVINGS"
        );

        mockMvc.perform(
                        get("/api/accounts")
                                .header(
                                        "Authorization",
                                        "Bearer " + userOneToken
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllAccounts_shouldAllowAdmin()
            throws Exception {

        createAccount(
                userOneToken,
                "ACC10007",
                "SAVINGS"
        );

        createAccount(
                userTwoToken,
                "ACC10008",
                "CURRENT"
        );

        mockMvc.perform(
                        get("/api/accounts")
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.length()")
                                .value(2)
                );
    }

    @Test
    void updateAccount_shouldReturnForbiddenForNormalUser()
            throws Exception {

        Long accountId = createAccount(
                userOneToken,
                "ACC10009",
                "SAVINGS"
        );

        String requestBody = """
                {
                    "accountType": "CURRENT"
                }
                """;

        mockMvc.perform(
                        put("/api/accounts/{id}", accountId)
                                .header(
                                        "Authorization",
                                        "Bearer " + userOneToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void updateAccount_shouldAllowAdminToChangeAccountType()
            throws Exception {

        Long accountId = createAccount(
                userOneToken,
                "ACC10010",
                "SAVINGS"
        );

        String requestBody = """
                {
                    "accountType": "CURRENT"
                }
                """;

        mockMvc.perform(
                        put("/api/accounts/{id}", accountId)
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(accountId)
                )
                .andExpect(
                        jsonPath("$.accountType")
                                .value("CURRENT")
                )
                .andExpect(
                        jsonPath("$.accountNumber")
                                .value("ACC10010")
                );
    }

    @Test
    void updateAccountStatus_shouldAllowAdminToBlockAccount()
            throws Exception {

        Long accountId = createAccount(
                userOneToken,
                "ACC10011",
                "SAVINGS"
        );

        String requestBody = """
                {
                    "status": "BLOCKED"
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
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("BLOCKED")
                );
    }

    @Test
    void updateAccountStatus_shouldAllowAdminToCloseZeroBalanceAccount()
            throws Exception {

        Long accountId = createAccount(
                userOneToken,
                "ACC10012",
                "SAVINGS"
        );

        String requestBody = """
                {
                    "status": "CLOSED"
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
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.balance")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("CLOSED")
                );
    }

    @Test
    void updateAccountStatus_shouldRejectReopeningClosedAccount()
            throws Exception {

        Long accountId = createAccount(
                userOneToken,
                "ACC10013",
                "SAVINGS"
        );

        changeAccountStatus(
                accountId,
                "CLOSED"
        );

        String requestBody = """
                {
                    "status": "ACTIVE"
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
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.status")
                                .value(409)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Conflict")
                );
    }


    // =========================================================
    // Helper Methods
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
            String accountNumber,
            String accountType
    ) throws Exception {

        String requestBody = """
                {
                    "accountNumber": "%s",
                    "accountType": "%s"
                }
                """.formatted(
                accountNumber,
                accountType
        );

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

    private void changeAccountStatus(
            Long accountId,
            String status
    ) throws Exception {

        String requestBody = """
                {
                    "status": "%s"
                }
                """.formatted(status);

        mockMvc.perform(
                        patch(
                                "/api/accounts/{id}/status",
                                accountId
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isOk());
    }
}