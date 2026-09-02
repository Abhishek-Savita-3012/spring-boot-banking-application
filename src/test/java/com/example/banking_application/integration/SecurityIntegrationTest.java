package com.example.banking_application.integration;

import com.example.banking_application.model.Role;
import com.example.banking_application.model.User;
import com.example.banking_application.repository.UserRepository;
import com.example.banking_application.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User normalUser;
    private User adminUser;

    @BeforeEach
    void setUp() {

        userRepository.deleteAll();

        normalUser = new User();
        normalUser.setName("Normal User");
        normalUser.setEmail("user@gmail.com");
        normalUser.setPassword(
                passwordEncoder.encode("password123")
        );
        normalUser.setRole(Role.USER);

        normalUser =
                userRepository.save(normalUser);

        adminUser = new User();
        adminUser.setName("Admin User");
        adminUser.setEmail("admin@gmail.com");
        adminUser.setPassword(
                passwordEncoder.encode("password123")
        );
        adminUser.setRole(Role.ADMIN);

        adminUser =
                userRepository.save(adminUser);
    }

    @Test
    void protectedEndpoint_shouldReturnUnauthorizedWithoutToken()
            throws Exception {

        mockMvc.perform(
                        get("/api/admin/test")
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    @Test
    void adminEndpoint_shouldReturnForbiddenForUserToken() throws Exception {

        String userToken =
                jwtService.generateToken(
                        normalUser.getEmail(),
                        normalUser.getRole().toString()
                );

        mockMvc.perform(
                        get("/api/admin/test")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
                )
                .andExpect(
                        status().isForbidden()
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Forbidden")
                );
    }

    @Test
    void adminEndpoint_shouldAllowAdminToken()
            throws Exception {

        String adminToken =
                jwtService.generateToken(
                        adminUser.getEmail(),
                        adminUser.getRole().toString()
                );

        mockMvc.perform(
                        get("/api/admin/test")
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content().string("Welcome Admin")
                );
    }

    @Test
    void protectedEndpoint_shouldReturnUnauthorizedForMalformedToken()
            throws Exception {

        mockMvc.perform(
                        get("/api/admin/test")
                                .header(
                                        "Authorization",
                                        "Bearer invalid.jwt.token"
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    @Test
    void userToken_shouldAuthenticateSuccessfully()
            throws Exception {

        String userToken =
                jwtService.generateToken(
                        normalUser.getEmail(),
                        normalUser.getRole().toString()
                );

        mockMvc.perform(
                        get("/api/admin/test")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
                )
                .andExpect(
                        status().isForbidden()
                );
    }
}