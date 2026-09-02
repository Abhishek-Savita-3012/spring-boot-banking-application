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
class TransactionIntegrationTest {

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

        // Delete child data first because transactions reference accounts,
        // and accounts reference users.
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
    void deposit_shouldIncreaseBalanceAndCreateTransaction()
            throws Exception {

        Long accountId = createAccount(
                userOneToken,
                "TXN10001",
                "SAVINGS"
        );

        String requestBody = """
                {
                    "amount": 500.00
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/transactions/{id}/deposit",
                                accountId
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + userOneToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.balance")
                                .value(500.00)
                )
                .andExpect(
                        jsonPath("$.id")
                                .value(accountId)
                );

        assertEquals(
                1,
                transactionRepository.count()
        );
    }

    @Test
    void withdraw_shouldDecreaseBalance()
            throws Exception {

        Long accountId = createAccount(
                userOneToken,
                "TXN10002",
                "SAVINGS"
        );

        deposit(
                accountId,
                userOneToken,
                "500.00"
        );

        String requestBody = """
                {
                    "amount": 200.00
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/transactions/{id}/withdraw",
                                accountId
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + userOneToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.balance")
                                .value(300.00)
                );

        assertEquals(
                2,
                transactionRepository.count()
        );
    }

    @Test
    void withdraw_shouldRejectInsufficientBalance()
            throws Exception {

        Long accountId = createAccount(
                userOneToken,
                "TXN10003",
                "SAVINGS"
        );

        deposit(
                accountId,
                userOneToken,
                "100.00"
        );

        String requestBody = """
                {
                    "amount": 500.00
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/transactions/{id}/withdraw",
                                accountId
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + userOneToken
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
                                .value("Insufficient Balance")
                );

        // Failed withdrawal must not create another ledger record.
        assertEquals(
                1,
                transactionRepository.count()
        );
    }

    @Test
    void deposit_shouldRejectDifferentUsersAccount()
            throws Exception {

        Long accountId = createAccount(
                userOneToken,
                "TXN10004",
                "SAVINGS"
        );

        String requestBody = """
                {
                    "amount": 100.00
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/transactions/{id}/deposit",
                                accountId
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + userTwoToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isForbidden());

        assertEquals(
                0,
                transactionRepository.count()
        );
    }

    @Test
    void transfer_shouldMoveMoneyBetweenAccounts()
            throws Exception {

        Long senderId = createAccount(
                userOneToken,
                "TXN10005",
                "SAVINGS"
        );

        Long receiverId = createAccount(
                userTwoToken,
                "TXN10006",
                "CURRENT"
        );

        deposit(
                senderId,
                userOneToken,
                "1000.00"
        );

        String requestBody = """
                {
                    "receiverAccountId": %d,
                    "amount": 300.00
                }
                """.formatted(receiverId);

        mockMvc.perform(
                        post(
                                "/api/transactions/{id}/transfer",
                                senderId
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + userOneToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(
                        content().string(
                                "Transfer completed successfully"
                        )
                );

        getAccountAndExpectBalance(
                senderId,
                userOneToken,
                700.00
        );

        getAccountAndExpectBalance(
                receiverId,
                userTwoToken,
                300.00
        );

        // 1 deposit + TRANSFER_OUT + TRANSFER_IN
        assertEquals(
                3,
                transactionRepository.count()
        );
    }

    @Test
    void transfer_shouldRejectSameSenderAndReceiver()
            throws Exception {

        Long accountId = createAccount(
                userOneToken,
                "TXN10007",
                "SAVINGS"
        );

        deposit(
                accountId,
                userOneToken,
                "500.00"
        );

        String requestBody = """
                {
                    "receiverAccountId": %d,
                    "amount": 100.00
                }
                """.formatted(accountId);

        mockMvc.perform(
                        post(
                                "/api/transactions/{id}/transfer",
                                accountId
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + userOneToken
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
                                .value("Invalid Transfer")
                );

        assertEquals(
                1,
                transactionRepository.count()
        );
    }

    @Test
    void transfer_shouldRejectInsufficientBalance()
            throws Exception {

        Long senderId = createAccount(
                userOneToken,
                "TXN10008",
                "SAVINGS"
        );

        Long receiverId = createAccount(
                userTwoToken,
                "TXN10009",
                "CURRENT"
        );

        deposit(
                senderId,
                userOneToken,
                "100.00"
        );

        String requestBody = """
                {
                    "receiverAccountId": %d,
                    "amount": 500.00
                }
                """.formatted(receiverId);

        mockMvc.perform(
                        post(
                                "/api/transactions/{id}/transfer",
                                senderId
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + userOneToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.error")
                                .value("Insufficient Balance")
                );

        getAccountAndExpectBalance(
                senderId,
                userOneToken,
                100.00
        );

        getAccountAndExpectBalance(
                receiverId,
                userTwoToken,
                0.00
        );
    }

    @Test
    void deposit_shouldRejectBlockedAccount()
            throws Exception {

        Long accountId = createAccount(
                userOneToken,
                "TXN10010",
                "SAVINGS"
        );

        changeAccountStatus(
                accountId,
                "BLOCKED"
        );

        String requestBody = """
                {
                    "amount": 100.00
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/transactions/{id}/deposit",
                                accountId
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + userOneToken
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

        assertEquals(
                0,
                transactionRepository.count()
        );
    }

    @Test
    void withdraw_shouldRejectBlockedAccount()
            throws Exception {

        Long accountId = createAccount(
                userOneToken,
                "TXN10011",
                "SAVINGS"
        );

        deposit(
                accountId,
                userOneToken,
                "200.00"
        );

        changeAccountStatus(
                accountId,
                "BLOCKED"
        );

        String requestBody = """
                {
                    "amount": 50.00
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/transactions/{id}/withdraw",
                                accountId
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + userOneToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isConflict());

        // Only original deposit should remain.
        assertEquals(
                1,
                transactionRepository.count()
        );
    }

    @Test
    void deposit_shouldRejectClosedAccount()
            throws Exception {

        Long accountId = createAccount(
                userOneToken,
                "TXN10012",
                "SAVINGS"
        );

        // New account has zero balance, so ADMIN can close it.
        changeAccountStatus(
                accountId,
                "CLOSED"
        );

        String requestBody = """
                {
                    "amount": 50.00
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/transactions/{id}/deposit",
                                accountId
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + userOneToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.error")
                                .value("Conflict")
                );

        assertEquals(
                0,
                transactionRepository.count()
        );
    }

    @Test
    void transactionHistory_shouldReturnAccountTransactions()
            throws Exception {

        Long accountId = createAccount(
                userOneToken,
                "TXN10013",
                "SAVINGS"
        );

        deposit(
                accountId,
                userOneToken,
                "500.00"
        );

        withdraw(
                accountId,
                userOneToken,
                "100.00"
        );

        mockMvc.perform(
                        get(
                                "/api/transactions/{id}/records",
                                accountId
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + userOneToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.transactions.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.currentPage")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.pageSize")
                                .value(10)
                )
                .andExpect(
                        jsonPath("$.first")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.last")
                                .value(true)
                );
    }

    @Test
    void transactionHistory_shouldFilterByTransactionType()
            throws Exception {

        Long accountId = createAccount(
                userOneToken,
                "TXN10014",
                "SAVINGS"
        );

        deposit(
                accountId,
                userOneToken,
                "500.00"
        );

        withdraw(
                accountId,
                userOneToken,
                "100.00"
        );

        mockMvc.perform(
                        get(
                                "/api/transactions/{id}/records",
                                accountId
                        )
                                .param(
                                        "type",
                                        "DEPOSIT"
                                )
                                .header(
                                        "Authorization",
                                        "Bearer " + userOneToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.transactions.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.transactions[0].transactionType"
                        )
                                .value("DEPOSIT")
                )
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(1)
                );
    }

    @Test
    void transactionHistory_shouldSupportPagination()
            throws Exception {

        Long accountId = createAccount(
                userOneToken,
                "TXN10015",
                "SAVINGS"
        );

        deposit(
                accountId,
                userOneToken,
                "100.00"
        );

        deposit(
                accountId,
                userOneToken,
                "100.00"
        );

        deposit(
                accountId,
                userOneToken,
                "100.00"
        );

        mockMvc.perform(
                        get(
                                "/api/transactions/{id}/records",
                                accountId
                        )
                                .param("page", "0")
                                .param("size", "2")
                                .header(
                                        "Authorization",
                                        "Bearer " + userOneToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.transactions.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.currentPage")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.pageSize")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(3)
                )
                .andExpect(
                        jsonPath("$.totalPages")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.first")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.last")
                                .value(false)
                );
    }

    @Test
    void transfer_shouldCreateTransferOutAndTransferInLedgerRecords()
            throws Exception {

        Long senderId = createAccount(
                userOneToken,
                "TXN10016",
                "SAVINGS"
        );

        Long receiverId = createAccount(
                userTwoToken,
                "TXN10017",
                "CURRENT"
        );

        deposit(
                senderId,
                userOneToken,
                "1000.00"
        );

        transfer(
                senderId,
                receiverId,
                userOneToken,
                "250.00"
        );

        // Sender history should contain TRANSFER_OUT.
        mockMvc.perform(
                        get(
                                "/api/transactions/{id}/records",
                                senderId
                        )
                                .param(
                                        "type",
                                        "TRANSFER_OUT"
                                )
                                .header(
                                        "Authorization",
                                        "Bearer " + userOneToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.transactions[0].transactionType"
                        )
                                .value("TRANSFER_OUT")
                )
                .andExpect(
                        jsonPath(
                                "$.transactions[0].amount"
                        )
                                .value(250.00)
                );

        // Receiver history should contain TRANSFER_IN.
        mockMvc.perform(
                        get(
                                "/api/transactions/{id}/records",
                                receiverId
                        )
                                .param(
                                        "type",
                                        "TRANSFER_IN"
                                )
                                .header(
                                        "Authorization",
                                        "Bearer " + userTwoToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.transactions[0].transactionType"
                        )
                                .value("TRANSFER_IN")
                )
                .andExpect(
                        jsonPath(
                                "$.transactions[0].amount"
                        )
                                .value(250.00)
                );
    }

    @Test
    void transaction_shouldReturnBadRequestForInvalidAmount()
            throws Exception {

        Long accountId = createAccount(
                userOneToken,
                "TXN10018",
                "SAVINGS"
        );

        String requestBody = """
                {
                    "amount": 0
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/transactions/{id}/deposit",
                                accountId
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + userOneToken
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

    private void deposit(
            Long accountId,
            String token,
            String amount
    ) throws Exception {

        String requestBody = """
                {
                    "amount": %s
                }
                """.formatted(amount);

        mockMvc.perform(
                        post(
                                "/api/transactions/{id}/deposit",
                                accountId
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isOk());
    }

    private void withdraw(
            Long accountId,
            String token,
            String amount
    ) throws Exception {

        String requestBody = """
                {
                    "amount": %s
                }
                """.formatted(amount);

        mockMvc.perform(
                        post(
                                "/api/transactions/{id}/withdraw",
                                accountId
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isOk());
    }

    private void transfer(
            Long senderAccountId,
            Long receiverAccountId,
            String token,
            String amount
    ) throws Exception {

        String requestBody = """
                {
                    "receiverAccountId": %d,
                    "amount": %s
                }
                """.formatted(
                receiverAccountId,
                amount
        );

        mockMvc.perform(
                        post(
                                "/api/transactions/{id}/transfer",
                                senderAccountId
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isOk());
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

    private void getAccountAndExpectBalance(
            Long accountId,
            String token,
            double expectedBalance
    ) throws Exception {

        mockMvc.perform(
                        get(
                                "/api/accounts/{id}",
                                accountId
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.balance")
                                .value(expectedBalance)
                );
    }
}