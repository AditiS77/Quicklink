package com.aditisingh.Quicklink.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class AdminController {

    @GetMapping("/admin")
    public RedirectView dashboard() {
        return new RedirectView("http://localhost:5173/admin");
    }
}
