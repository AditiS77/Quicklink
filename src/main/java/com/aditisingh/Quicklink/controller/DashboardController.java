package com.aditisingh.Quicklink.controller;

import com.aditisingh.Quicklink.service.ShortUrlService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final ShortUrlService shortUrlService;

    public DashboardController(ShortUrlService shortUrlService) {
        this.shortUrlService = shortUrlService;
    }

    @GetMapping("/my-urls")
    public String myUrls(@AuthenticationPrincipal UserDetails user, Model model) {
        model.addAttribute("shortUrls", shortUrlService.findUrlsForOwner(user.getUsername()));
        return "my-urls";
    }
}
