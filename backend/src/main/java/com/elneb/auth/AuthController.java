package com.elneb.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service) { this.service = service; }
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthService.AuthResponse register(@Valid @RequestBody AuthService.RegisterRequest request) { return service.register(request); }
    @PostMapping("/register/college")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthService.AuthResponse registerCollege(@Valid @RequestBody AuthService.CollegeRegistrationRequest request) {
        return service.registerCollege(request);
    }
    @PostMapping("/login")
    public AuthService.AuthResponse login(@Valid @RequestBody AuthService.LoginRequest request) { return service.login(request); }

    @GetMapping("/me")
    public AuthService.AuthResponse me(Authentication authentication) {
        return service.currentUser(UUID.fromString(authentication.getName()));
    }
}
