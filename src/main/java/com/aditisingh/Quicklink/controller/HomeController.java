package com.aditisingh.Quicklink.controller;

import com.aditisingh.Quicklink.dto.CreateShortUrlRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @PostMapping("/shorten")
    public String shortenUrl(CreateShortUrlRequest request) {

        System.out.println("URL Submitted: " + request.getOriginalUrl());

        return "index";
    }
}