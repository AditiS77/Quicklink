package com.aditisingh.Quicklink.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class DashboardController {

    @GetMapping("/my-urls")
    public RedirectView myUrls() {
        return new RedirectView("http://localhost:5173/my-urls");
    }
}
