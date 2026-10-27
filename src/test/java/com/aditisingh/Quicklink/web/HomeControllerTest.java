package com.aditisingh.Quicklink.web;

import com.aditisingh.Quicklink.config.SecurityConfig;
import com.aditisingh.Quicklink.controller.HomeController;
import com.aditisingh.Quicklink.exception.ExpiredShortUrlException;
import com.aditisingh.Quicklink.exception.PrivateUrlAccessDeniedException;
import com.aditisingh.Quicklink.service.ShortUrlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HomeController.class)
@Import(SecurityConfig.class)
class HomeControllerTest {

    private static final String CODE = "Ab3dE6gH";
    private static final String TARGET = "https://example.com/target";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShortUrlService shortUrlService;

    @Test
    void shortCodeRedirectsToItsDestination() throws Exception {
        when(shortUrlService.resolveAndRecordClick(CODE, null)).thenReturn(Optional.of(TARGET));

        mockMvc.perform(get("/" + CODE))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(TARGET));
    }

    @Test
    void unknownCodeReturns404Message() throws Exception {
        when(shortUrlService.resolveAndRecordClick(CODE, null)).thenReturn(Optional.empty());

        mockMvc.perform(get("/" + CODE))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("This short link does not exist.")));
    }

    @Test
    void expiredCodeReturns410Message() throws Exception {
        when(shortUrlService.resolveAndRecordClick(CODE, null)).thenThrow(new ExpiredShortUrlException());

        mockMvc.perform(get("/" + CODE))
                .andExpect(status().isGone())
                .andExpect(content().string(containsString("This short link has expired.")));
    }

    @Test
    void privateCodeRemainsForbiddenForAnonymousVisitors() throws Exception {
        when(shortUrlService.resolveAndRecordClick(CODE, null)).thenThrow(new PrivateUrlAccessDeniedException());

        mockMvc.perform(get("/" + CODE)).andExpect(status().isForbidden());
    }
}
