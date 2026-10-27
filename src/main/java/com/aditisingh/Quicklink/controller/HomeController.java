package com.aditisingh.Quicklink.controller;

import com.aditisingh.Quicklink.exception.ExpiredShortUrlException;
import com.aditisingh.Quicklink.service.ShortUrlService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class HomeController {

    private final ShortUrlService shortUrlService;

    public HomeController(ShortUrlService shortUrlService) {
        this.shortUrlService = shortUrlService;
    }

    /** Direct visits to the backend root lead to the separate React development server. */
    @GetMapping("/")
    public RedirectView home() {
        return new RedirectView("http://localhost:5173/");
    }

    @GetMapping("/{shortCode:[A-Za-z0-9]{8}}")
    public Object redirectToOriginalUrl(@PathVariable String shortCode,
                                        @AuthenticationPrincipal UserDetails user) {
        String requesterEmail = user == null ? null : user.getUsername();
        return shortUrlService.resolveAndRecordClick(shortCode, requesterEmail)
                .<Object>map(RedirectView::new)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("This short link does not exist."));
    }

    @ExceptionHandler(ExpiredShortUrlException.class)
    public ResponseEntity<String> expiredUrl() {
        return ResponseEntity.status(HttpStatus.GONE).body("This short link has expired.");
    }
}
