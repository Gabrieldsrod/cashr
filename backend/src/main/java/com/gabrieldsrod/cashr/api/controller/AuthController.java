package com.gabrieldsrod.cashr.api.controller;

import com.gabrieldsrod.cashr.api.dto.request.AuthLoginRequest;
import com.gabrieldsrod.cashr.api.dto.request.AuthRegisterRequest;
import com.gabrieldsrod.cashr.api.dto.response.AuthTokenResponse;
import com.gabrieldsrod.cashr.api.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthTokenResponse> register(@Valid @RequestBody AuthRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponse> login(@Valid @RequestBody AuthLoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
