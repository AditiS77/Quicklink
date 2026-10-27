package com.aditisingh.Quicklink.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

/** Legacy page paths hand off to the standalone React application. */
@Controller
public class AuthController {

    @GetMapping("/login")
    public RedirectView login() {
        return new RedirectView("http://localhost:5173/login");
    }

    @GetMapping("/register")
    public RedirectView register() {
        return new RedirectView("http://localhost:5173/register");
    }
}
