package com.aditisingh.Quicklink.controller;

import com.aditisingh.Quicklink.dto.RegistrationRequest;
import com.aditisingh.Quicklink.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registrationForm(Model model) {
        model.addAttribute("registrationRequest", new RegistrationRequest());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute RegistrationRequest registrationRequest,
                               BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            userService.registerUser(
                    registrationRequest.getName(),
                    registrationRequest.getEmail(),
                    registrationRequest.getPassword());
        } catch (IllegalArgumentException exception) {
            bindingResult.rejectValue("email", "duplicate", exception.getMessage());
            return "register";
        }

        return "redirect:/login?registered";
    }
}
