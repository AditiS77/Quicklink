package com.aditisingh.Quicklink.controller;

import com.aditisingh.Quicklink.dto.CreateShortUrlRequest;
import com.aditisingh.Quicklink.dto.RegistrationRequest;
import com.aditisingh.Quicklink.entity.ShortUrl;
import com.aditisingh.Quicklink.entity.User;
import com.aditisingh.Quicklink.service.ShortUrlService;
import com.aditisingh.Quicklink.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/** Small JSON adapter for the separate React frontend; business rules remain in the services. */
@RestController
@RequestMapping("/api")
public class FrontendApiController {

    private final ShortUrlService shortUrlService;
    private final UserService userService;

    public FrontendApiController(ShortUrlService shortUrlService, UserService userService) {
        this.shortUrlService = shortUrlService;
        this.userService = userService;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleApiError(ResponseStatusException exception) {
        return ProblemDetail.forStatusAndDetail(exception.getStatusCode(), exception.getReason());
    }

    @GetMapping("/session")
    public Map<String, Object> session(Authentication authentication, CsrfToken csrfToken) {
        boolean authenticated = authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserDetails;
        UserDetails user = authenticated ? (UserDetails) authentication.getPrincipal() : null;
        return Map.of(
                "authenticated", authenticated,
                "name", user == null ? "" : userService.findNameByEmail(user.getUsername()),
                "email", user == null ? "" : user.getUsername(),
                "admin", user != null && user.getAuthorities().stream()
                        .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")),
                "csrfHeader", csrfToken.getHeaderName(),
                "csrfToken", csrfToken.getToken());
    }

    @PostMapping("/register")
    public Map<String, String> register(@Valid @RequestBody RegistrationRequest request, BindingResult errors) {
        if (errors.hasErrors()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    errors.getFieldErrors().stream().map(error -> error.getDefaultMessage()).findFirst()
                            .orElse("Please check your details."));
        }
        try {
            userService.registerUser(request.getName(), request.getEmail(), request.getPassword());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
        return Map.of("message", "Account created successfully.");
    }

    @PostMapping("/shorten")
    public Map<String, Object> shorten(@Valid @RequestBody CreateShortUrlRequest request,
                                       BindingResult errors,
                                       @AuthenticationPrincipal UserDetails user) {
        if (errors.hasErrors()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    errors.getFieldErrors().stream().map(error -> error.getDefaultMessage()).findFirst()
                            .orElse("Please check the URL details."));
        }
        ShortUrl created = shortUrlService.createShortUrl(request.getOriginalUrl(), request.getExpiresAt(),
                request.getVisibility(), user.getUsername());
        return Map.of("shortCode", created.getShortCode(), "originalUrl", created.getOriginalUrl(),
                "visibility", created.getVisibility(), "expiresAt",
                created.getExpiresAt() == null ? "Never" : created.getExpiresAt().toString());
    }

    @GetMapping("/my-urls")
    public List<Map<String, Object>> myUrls(@AuthenticationPrincipal UserDetails user) {
        return shortUrlService.findUrlsForOwner(user.getUsername()).stream()
                .map(url -> urlView(url, false)).toList();
    }

    @GetMapping("/admin")
    public Map<String, Object> admin() {
        List<Map<String, Object>> users = userService.findAllUsers().stream().map(this::userView).toList();
        List<Map<String, Object>> urls = shortUrlService.findAllUrls().stream()
                .map(url -> urlView(url, true)).toList();
        return Map.of("users", users, "shortUrls", urls);
    }

    private Map<String, Object> userView(User user) {
        return Map.of("name", user.getName(), "email", user.getEmail(), "role", user.getRole());
    }

    private Map<String, Object> urlView(ShortUrl url, boolean includeOwner) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("shortCode", url.getShortCode());
        view.put("originalUrl", url.getOriginalUrl());
        view.put("clickCount", url.getClickCount());
        view.put("visibility", url.getVisibility());
        view.put("expiresAt", url.getExpiresAt() == null ? "Never" : url.getExpiresAt().toString());
        view.put("createdAt", url.getCreatedAt().toString());
        if (includeOwner) {
            view.put("ownerEmail", url.getOwner().getEmail());
        }
        return view;
    }
}
