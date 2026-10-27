package com.aditisingh.Quicklink.web;

import com.aditisingh.Quicklink.config.SecurityConfig;
import com.aditisingh.Quicklink.controller.FrontendApiController;
import com.aditisingh.Quicklink.entity.Role;
import com.aditisingh.Quicklink.entity.ShortUrl;
import com.aditisingh.Quicklink.entity.User;
import com.aditisingh.Quicklink.entity.Visibility;
import com.aditisingh.Quicklink.service.ShortUrlService;
import com.aditisingh.Quicklink.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FrontendApiController.class)
@Import(SecurityConfig.class)
class FrontendApiControllerTest {

    private static final String EMAIL = "ada@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ShortUrlService shortUrlService;

    @MockitoBean
    private UserService userService;

    @Test
    void sessionEndpointReturnsAnonymousStateAndCsrfToken() throws Exception {
        mockMvc.perform(get("/api/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(jsonPath("$.csrfHeader").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.csrfToken").isNotEmpty());
    }

    @Test
    @WithMockUser(username = EMAIL)
    void sessionEndpointReturnsSignedInUserDetails() throws Exception {
        when(userService.findNameByEmail(EMAIL)).thenReturn("Ada");

        mockMvc.perform(get("/api/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.name").value("Ada"))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.admin").value(false));
    }

    @Test
    void registrationUsesExistingUserServiceAndReturnsJson() throws Exception {
        MvcResult sessionResult = mockMvc.perform(get("/api/session"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode session = objectMapper.readTree(sessionResult.getResponse().getContentAsString());

        mockMvc.perform(post("/api/register")
                        .session((MockHttpSession) sessionResult.getRequest().getSession(false))
                        .header(session.get("csrfHeader").asText(), session.get("csrfToken").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ada","email":"ada@example.com","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account created successfully."));

        verify(userService).registerUser("Ada", EMAIL, "password123");
    }

    @Test
    void invalidRegistrationReturnsBadRequestWithoutCallingService() throws Exception {
        mockMvc.perform(post("/api/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","email":"bad","password":"short"}
                                """))
                .andExpect(status().isBadRequest());

        verify(userService, never()).registerUser(any(), any(), any());
    }

    @Test
    void duplicateRegistrationReturnsJsonConflictMessage() throws Exception {
        doThrow(new IllegalArgumentException("An account already exists for this email address"))
                .when(userService).registerUser("Ada", EMAIL, "password123");

        mockMvc.perform(post("/api/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ada","email":"ada@example.com","password":"password123"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("An account already exists for this email address"));
    }

    @Test
    void registrationRejectsRequestsWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shorteningRequiresAnAuthenticatedSession() throws Exception {
        mockMvc.perform(post("/api/shorten").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrl\":\"https://example.com\",\"visibility\":\"PUBLIC\"}"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = EMAIL)
    void shorteningValidatesInputAndReturnsTheCreatedCode() throws Exception {
        ShortUrl created = mock(ShortUrl.class);
        when(created.getShortCode()).thenReturn("Ab3dE6gH");
        when(created.getOriginalUrl()).thenReturn("https://example.com");
        when(created.getVisibility()).thenReturn(Visibility.PUBLIC);
        when(created.getExpiresAt()).thenReturn(null);
        when(shortUrlService.createShortUrl("https://example.com", null, Visibility.PUBLIC, EMAIL))
                .thenReturn(created);

        mockMvc.perform(post("/api/shorten").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrl\":\"https://example.com\",\"visibility\":\"PUBLIC\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("Ab3dE6gH"))
                .andExpect(jsonPath("$.visibility").value("PUBLIC"));
    }

    @Test
    @WithMockUser(username = EMAIL)
    void usersCanOnlyGetTheirOwnUrlList() throws Exception {
        ShortUrl url = mock(ShortUrl.class);
        when(url.getShortCode()).thenReturn("Ab3dE6gH");
        when(url.getOriginalUrl()).thenReturn("https://example.com");
        when(url.getClickCount()).thenReturn(3L);
        when(url.getVisibility()).thenReturn(Visibility.PRIVATE);
        when(url.getExpiresAt()).thenReturn(null);
        when(url.getCreatedAt()).thenReturn(java.time.Instant.parse("2026-10-27T00:00:00Z"));
        when(shortUrlService.findUrlsForOwner(EMAIL)).thenReturn(List.of(url));

        mockMvc.perform(get("/api/my-urls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].shortCode").value("Ab3dE6gH"))
                .andExpect(jsonPath("$[0].clickCount").value(3))
                .andExpect(jsonPath("$[0].visibility").value("PRIVATE"));

        verify(shortUrlService).findUrlsForOwner(EMAIL);
    }

    @Test
    @WithMockUser(roles = "USER")
    void ordinaryUsersCannotReadAdminApi() throws Exception {
        mockMvc.perform(get("/api/admin")).andExpect(status().isForbidden());
        verify(userService, never()).findAllUsers();
    }

    @Test
    @WithMockUser(username = "root@example.com", roles = "ADMIN")
    void adminsReceiveUserAndUrlJson() throws Exception {
        User owner = new User("Ada", EMAIL, "hash", Role.USER);
        User admin = new User("Root", "root@example.com", "hash", Role.ADMIN);
        ShortUrl url = mock(ShortUrl.class);
        when(url.getShortCode()).thenReturn("Ab3dE6gH");
        when(url.getOriginalUrl()).thenReturn("https://example.com");
        when(url.getClickCount()).thenReturn(0L);
        when(url.getVisibility()).thenReturn(Visibility.PUBLIC);
        when(url.getExpiresAt()).thenReturn(null);
        when(url.getCreatedAt()).thenReturn(java.time.Instant.parse("2026-10-27T00:00:00Z"));
        when(url.getOwner()).thenReturn(owner);
        when(userService.findAllUsers()).thenReturn(List.of(owner, admin));
        when(shortUrlService.findAllUrls()).thenReturn(List.of(url));

        mockMvc.perform(get("/api/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users.length()").value(2))
                .andExpect(jsonPath("$.shortUrls[0].shortCode").value("Ab3dE6gH"))
                .andExpect(jsonPath("$.shortUrls[0].ownerEmail").value(EMAIL));
    }
}
