package com.fsmonitor.app.security;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end authorization matrix: verifies 401 for anonymous callers,
 * 403 for non-admin mutations on protected surfaces, admin access, the login
 * rate limiter, and JWT revocation via tokenVersion.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SecurityAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String adminToken;
    private String userToken;

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        String token = body.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
        assertThat(token).isNotBlank();
        return token;
    }

    @BeforeAll
    void setUp() throws Exception {
        // DataInitializer seeds admin with FS_MONITOR_ADMIN_PASSWORD (AdminPass1 in test config)
        adminToken = login("admin", "AdminPass1");

        // Admin creates a regular user (register is admin-only)
        mockMvc.perform(post("/api/auth/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"regular\",\"email\":\"regular@example.com\",\"password\":\"UserPass1\"}"))
                .andExpect(status().isOk());

        userToken = login("regular", "UserPass1");
    }

    // ---------- anonymous callers: 401 everywhere on /api ----------

    @Test
    @Order(1)
    void unauthenticatedRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/integrations")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/services")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/delete-services")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/log-configs")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/services").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/delete-services").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/file-browser/roots")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/system/stats")).andExpect(status().isUnauthorized());
    }

    @Test
    @Order(2)
    void publicEndpointsRemainPublic() throws Exception {
        mockMvc.perform(get("/api/version")).andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"nobody\",\"password\":\"wrong1\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- regular user: read ok, admin surfaces forbidden ----------

    @Test
    @Order(3)
    void regularUserCanReadMonitoringData() throws Exception {
        mockMvc.perform(get("/api/integrations").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/services").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/log-configs").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/delete-services").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(4)
    void regularUserCannotMutateConfigurations() throws Exception {
        mockMvc.perform(post("/api/services")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/delete-services")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/log-configs")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        // Note: bean validation runs before method security, so an invalid body
        // yields 400 rather than 403 - a valid body still must not pass admin checks
        mockMvc.perform(post("/api/integrations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"path\":\"/tmp\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(5)
    void regularUserCannotReachAdministrativeSurfaces() throws Exception {
        mockMvc.perform(get("/api/file-browser/roots").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/system/stats").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/services/test-connection")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"host\":\"localhost\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/auth/register")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"hacker\",\"email\":\"h@x.com\",\"password\":\"Hacker123\"}"))
                .andExpect(status().isForbidden());
    }

    // ---------- admin: allowed ----------

    @Test
    @Order(6)
    void adminCanReachAdministrativeSurfaces() throws Exception {
        mockMvc.perform(get("/api/file-browser/roots").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/system/stats").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    // ---------- password policy ----------

    @Test
    @Order(7)
    void weakPasswordIsRejectedOnRegister() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"weak\",\"email\":\"w@x.com\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/auth/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"weak2\",\"email\":\"w2@x.com\",\"password\":\"allletters\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- login rate limiting ----------

    @Test
    @Order(8)
    void loginIsThrottledAfterFiveFailures() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"usernameOrEmail\":\"throttle-target\",\"password\":\"bad-pass-1\"}"))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"throttle-target\",\"password\":\"bad-pass-1\"}"))
                .andExpect(status().isTooManyRequests());
    }

    // ---------- JWT revocation via tokenVersion ----------

    @Test
    @Order(9)
    void passwordChangeInvalidatesPreviousTokens() throws Exception {
        // Old token works before the change
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        MvcResult change = mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"UserPass1\",\"newPassword\":\"UserPass2\",\"confirmPassword\":\"UserPass2\"}"))
                .andExpect(status().isOk())
                .andReturn();

        // The pre-change token is now rejected
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isUnauthorized());

        // The newly issued token works
        String newToken = change.getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk());

        userToken = newToken;
    }

    // ---------- DTO contract: no secrets in responses ----------

    @Test
    @Order(10)
    void userResponsesNeverExposePassword() throws Exception {
        MvcResult me = mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(me.getResponse().getContentAsString()).doesNotContain("\"password\"");
        assertThat(me.getResponse().getContentAsString()).doesNotContain("$2a$");
    }
}
