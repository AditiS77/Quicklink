package com.aditisingh.Quicklink.controller;

import com.aditisingh.Quicklink.dto.CreateShortUrlRequest;
import com.aditisingh.Quicklink.service.ShortUrlService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class HomeController {

    private final ShortUrlService shortUrlService;

    public HomeController(ShortUrlService shortUrlService) {
        this.shortUrlService = shortUrlService;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @PostMapping("/shorten")
    public String shortenUrl(CreateShortUrlRequest request) {
        shortUrlService.createShortUrl(request.getOriginalUrl());
        return "index";
    }

    @GetMapping("/{shortCode}")
    public ModelAndView redirectToOriginalUrl(@PathVariable String shortCode) {
        return shortUrlService.findOriginalUrl(shortCode)
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
}
