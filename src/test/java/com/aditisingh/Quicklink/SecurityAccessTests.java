package com.aditisingh.Quicklink;

import com.aditisingh.Quicklink.support.AbstractPostgresTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verifies the API security rules and React handoff routes. */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityAccessTests extends AbstractPostgresTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sessionApiIsAvailableToAnonymousVisitors() throws Exception {
        mockMvc.perform(get("/api/session")).andExpect(status().isOk());
    }

    @Test
    void loginAndRegisterPagesRedirectToTheReactFrontend() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:5173/login"));
        mockMvc.perform(get("/register"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:5173/register"));
    }

    @Test
    void protectedApiCallsSendAnonymousVisitorsToLogin() throws Exception {
        mockMvc.perform(get("/api/my-urls"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(post("/api/shorten").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void registrationApiRejectsRequestsWithoutCsrf() throws Exception {
        mockMvc.perform(post("/api/register").contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void ordinaryUsersCannotReadAdminApi() throws Exception {
        mockMvc.perform(get("/api/admin")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminsCanReadAdminApi() throws Exception {
        mockMvc.perform(get("/api/admin")).andExpect(status().isOk());
    }
}
