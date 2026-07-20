package com.aditisingh.Quicklink.controller;

import com.aditisingh.Quicklink.dto.CreateShortUrlRequest;
import com.aditisingh.Quicklink.entity.ShortUrl;
import com.aditisingh.Quicklink.exception.ExpiredShortUrlException;
import com.aditisingh.Quicklink.service.ShortUrlService;
import com.aditisingh.Quicklink.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class HomeController {

    private static final Logger LOGGER = LoggerFactory.getLogger(HomeController.class);

    private final ShortUrlService shortUrlService;
    private final UserService userService;

    public HomeController(ShortUrlService shortUrlService, UserService userService) {
        this.shortUrlService = shortUrlService;
        this.userService = userService;
    }

    @GetMapping("/")
    public String home(Model model, @AuthenticationPrincipal UserDetails user) {
        addCurrentUserAttributes(model, user);
        model.addAttribute("createShortUrlRequest", new CreateShortUrlRequest());
        return "index";
    }

    @PostMapping("/shorten")
    public String shortenUrl(@Valid @ModelAttribute("createShortUrlRequest") CreateShortUrlRequest request,
                             BindingResult bindingResult,
                             @AuthenticationPrincipal UserDetails user,
                             Model model) {
        addCurrentUserAttributes(model, user);
        if (bindingResult.hasErrors()) {
            return "index";
        }
        if (user == null) {
            model.addAttribute("creationError", "Please log in before creating a short URL.");
            return "index";
        }

        try {
            ShortUrl shortUrl = shortUrlService.createShortUrl(
                    request.getOriginalUrl(), request.getExpiresAt(), request.getVisibility(), user.getUsername());
            model.addAttribute("generatedShortUrl", buildShortUrl(shortUrl.getShortCode()));
            model.addAttribute("successMessage", "Short URL created successfully.");
        } catch (IllegalArgumentException exception) {
            model.addAttribute("creationError", "Invalid URL. Please check the details and try again.");
        } catch (Exception exception) {
            LOGGER.error("Unable to create a short URL for authenticated user {}", user.getUsername(), exception);
            model.addAttribute("creationError", "Creation failed. Please try again.");
        }

        return "index";
    }

    @GetMapping("/{shortCode}")
    public ModelAndView redirectToOriginalUrl(@PathVariable String shortCode,
            @AuthenticationPrincipal UserDetails user) {
        String requesterEmail = user == null ? null : user.getUsername();
        return shortUrlService.resolveAndRecordClick(shortCode, requesterEmail)
                .map(this::redirectTo)
                .orElseGet(this::notFound);
    }

    private ModelAndView redirectTo(String originalUrl) {
        RedirectView redirectView = new RedirectView(originalUrl);
        redirectView.setExposeModelAttributes(false);
        return new ModelAndView(redirectView);
    }

    private ModelAndView notFound() {
        ModelAndView notFoundPage = new ModelAndView("404");
        notFoundPage.setStatus(HttpStatus.NOT_FOUND);
        return notFoundPage;
    }

    @ExceptionHandler(ExpiredShortUrlException.class)
    public ModelAndView expiredUrl() {
        ModelAndView expiredPage = new ModelAndView("404");
        expiredPage.setStatus(HttpStatus.GONE);
        expiredPage.addObject("errorMessage", "This short link has expired.");
        return expiredPage;
    }

    private String buildShortUrl(String shortCode) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/{shortCode}")
                .buildAndExpand(shortCode)
                .toUriString();
    }

    private void addCurrentUserAttributes(Model model, UserDetails user) {
        if (user == null) {
            return;
        }

        model.addAttribute("currentUserName", userService.findNameByEmail(user.getUsername()));
        model.addAttribute("isAdmin", user.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));
    }
}
