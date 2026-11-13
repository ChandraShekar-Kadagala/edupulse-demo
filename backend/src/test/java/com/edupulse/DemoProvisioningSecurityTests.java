package com.edupulse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 1.1 regression tests: demo provisioning must never be reachable anonymously,
 * and the public auth surface must be an explicit allow-list.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {"app.seeder.enabled=false"})
class DemoProvisioningSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Legacy public provisioning URL is no longer reachable anonymously")
    void legacyProvisioningUrlRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/auth/provision-demo-showcase"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Anonymous caller cannot provision demo data")
    void anonymousCannotProvision() throws Exception {
        mockMvc.perform(post("/api/admin/demo/provision"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("Student cannot provision demo data")
    void studentCannotProvision() throws Exception {
        mockMvc.perform(post("/api/admin/demo/provision"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "FACULTY")
    @DisplayName("Faculty cannot provision demo data")
    void facultyCannotProvision() throws Exception {
        mockMvc.perform(post("/api/admin/demo/provision"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Admin gets 409 and no data change when seeding is disabled")
    void adminGetsConflictWhenSeederDisabled() throws Exception {
        mockMvc.perform(post("/api/admin/demo/provision"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Public auth endpoints stay public; unknown /api/auth paths do not")
    void publicAuthAllowListIsExplicit() throws Exception {
        mockMvc.perform(get("/api/auth/demo-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.facultyList[*].username", not(hasItem("admin"))));
        mockMvc.perform(get("/api/auth/some-unlisted-endpoint"))
                .andExpect(status().isUnauthorized());
    }
}
