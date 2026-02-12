package com.ordenatec.portallicitaciones.api.controller;

import com.ordenatec.portallicitaciones.application.auth.AuthService;
import com.ordenatec.portallicitaciones.application.auth.dto.AuthResponse;
import com.ordenatec.portallicitaciones.application.auth.dto.LoginRequest;
import com.ordenatec.portallicitaciones.application.auth.dto.RegisterRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody RegisterRequest req) {
        authService.register(req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }
}
