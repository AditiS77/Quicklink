package com.aditisingh.Quicklink.controller;

import com.aditisingh.Quicklink.service.ShortUrlService;
import com.aditisingh.Quicklink.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    private final UserService userService;
    private final ShortUrlService shortUrlService;

    public AdminController(UserService userService, ShortUrlService shortUrlService) {
        this.userService = userService;
        this.shortUrlService = shortUrlService;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        model.addAttribute("users", userService.findAllUsers());
        model.addAttribute("shortUrls", shortUrlService.findAllUrls());
        return "admin";
    }
}
