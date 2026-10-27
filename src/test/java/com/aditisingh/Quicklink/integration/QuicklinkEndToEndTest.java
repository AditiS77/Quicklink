package com.aditisingh.Quicklink.integration;

import com.aditisingh.Quicklink.entity.Role;
import com.aditisingh.Quicklink.entity.ShortUrl;
import com.aditisingh.Quicklink.entity.User;
import com.aditisingh.Quicklink.entity.Visibility;
import com.aditisingh.Quicklink.repository.ShortUrlRepository;
import com.aditisingh.Quicklink.repository.UserRepository;
import com.aditisingh.Quicklink.service.ShortUrlService;
import com.aditisingh.Quicklink.service.UserService;
import com.aditisingh.Quicklink.support.AbstractPostgresTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives the real application (security filters, controllers, services, JPA, Flyway, Postgres)
 * through complete user journeys. Nothing is mocked.
 */
@SpringBootTest
@AutoConfigureMockMvc
class QuicklinkEndToEndTest extends AbstractPostgresTest {

    private static final String PASSWORD = "password123";
    private static final String TARGET = "https://example.com/some/long/path?x=1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShortUrlRepository shortUrlRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ShortUrlService shortUrlService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        shortUrlRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    void registerLoginShortenRedirectAndTrackClicks() throws Exception {
        register("Ada", "ada@example.com");

        User stored = userRepository.findByEmail("ada@example.com").orElseThrow();
        assertThat(stored.getPassword()).startsWith("$2").isNotEqualTo(PASSWORD);
        assertThat(stored.getRole()).isEqualTo(Role.USER);

        MockHttpSession session = login("ada@example.com", PASSWORD);
        String code = createLink(session, TARGET, Visibility.PUBLIC);

        // Anyone, even anonymous, can follow a public link.
        mockMvc.perform(get("/" + code)).andExpect(status().isFound()).andExpect(redirectedUrl(TARGET));
        mockMvc.perform(get("/" + code)).andExpect(status().isFound()).andExpect(redirectedUrl(TARGET));

        assertThat(shortUrlRepository.findByShortCode(code).orElseThrow().getClickCount()).isEqualTo(2);

        mockMvc.perform(get("/api/my-urls").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(code)));
    }

    @Test
    void privateLinksWorkOnlyForTheirOwner() throws Exception {
        register("Ada", "ada@example.com");
        register("Bob", "bob@example.com");
        MockHttpSession adaSession = login("ada@example.com", PASSWORD);
        MockHttpSession bobSession = login("bob@example.com", PASSWORD);
        String code = createLink(adaSession, TARGET, Visibility.PRIVATE);

        mockMvc.perform(get("/" + code)).andExpect(status().isForbidden());
        mockMvc.perform(get("/" + code).session(bobSession)).andExpect(status().isForbidden());
        mockMvc.perform(get("/" + code).session(adaSession))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(TARGET));

        // Only the owner's successful visit was counted.
        assertThat(shortUrlRepository.findByShortCode(code).orElseThrow().getClickCount()).isEqualTo(1);
    }

    @Test
    void usersOnlySeeTheirOwnLinksOnMyUrls() throws Exception {
        register("Ada", "ada@example.com");
        register("Bob", "bob@example.com");
        MockHttpSession adaSession = login("ada@example.com", PASSWORD);
        MockHttpSession bobSession = login("bob@example.com", PASSWORD);
        String adaCode = createLink(adaSession, TARGET, Visibility.PUBLIC);
        String bobCode = createLink(bobSession, "https://example.org/bob", Visibility.PUBLIC);

        mockMvc.perform(get("/api/my-urls").session(adaSession))
                .andExpect(content().string(containsString(adaCode)))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString(bobCode))));
    }

    @Test
    void expiredLinksReturn410AndAreNotCounted() throws Exception {
        userService.registerUser("Ada", "ada@example.com", PASSWORD);
        ShortUrl expired = shortUrlService.createShortUrl("https://example.com",
                LocalDateTime.now(ZoneOffset.UTC).minusHours(1), Visibility.PUBLIC, "ada@example.com");

        mockMvc.perform(get("/" + expired.getShortCode()))
                .andExpect(status().isGone())
                .andExpect(content().string(containsString("This short link has expired.")));

        assertThat(shortUrlRepository.findByShortCode(expired.getShortCode()).orElseThrow().getClickCount()).isZero();
    }

    @Test
    void emailsAreTreatedCaseInsensitivelyAtRegistrationAndLogin() throws Exception {
        register("Ada", "ada@example.com");

        mockMvc.perform(post("/api/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Imposter", "email", "ADA@Example.com", "password", PASSWORD))))
                .andExpect(status().isConflict());

        assertThat(userRepository.count()).isEqualTo(1);
        mockMvc.perform(formLogin().user("ADA@Example.com").password(PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void wrongPasswordsAndUnknownUsersAreBothRejectedTheSameWay() throws Exception {
        register("Ada", "ada@example.com");

        mockMvc.perform(formLogin().user("ada@example.com").password("wrong-password"))
                .andExpect(redirectedUrl("/login?error"));
        mockMvc.perform(formLogin().user("ghost@example.com").password(PASSWORD))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void onlyAdminsCanOpenTheAdminDashboardAndTheyCanSeeEveryonesLinks() throws Exception {
        register("Ada", "ada@example.com");
        userRepository.save(new User("Root", "root@example.com", passwordEncoder.encode(PASSWORD), Role.ADMIN));
        MockHttpSession adaSession = login("ada@example.com", PASSWORD);
        MockHttpSession rootSession = login("root@example.com", PASSWORD);
        String code = createLink(adaSession, TARGET, Visibility.PRIVATE);

        mockMvc.perform(get("/api/admin").session(adaSession)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin").session(rootSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ada@example.com")))
                .andExpect(content().string(containsString(code)));
    }

    // ------------------------------------------------------------------ helpers

    private void register(String name, String email) throws Exception {
        mockMvc.perform(post("/api/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", name, "email", email, "password", PASSWORD))))
                .andExpect(status().isOk());
    }

    private MockHttpSession login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(formLogin().user(email).password(password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String createLink(MockHttpSession session, String url, Visibility visibility) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/shorten").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("originalUrl", url, "visibility", visibility.name()))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("shortCode").asText();
    }

    private String json(Map<String, String> values) throws Exception {
        return objectMapper.writeValueAsString(values);
    }
}
