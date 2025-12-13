package com.paypeek.backend.controller;

import com.paypeek.backend.dto.AuthResponse;
import com.paypeek.backend.dto.LoginDto;
import com.paypeek.backend.dto.SignupDto;
import com.paypeek.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

        private final AuthService authService;

        @PostMapping("/register")
        public ResponseEntity<AuthResponse> register(@Valid @RequestBody SignupDto request) {
                return ResponseEntity.ok(authService.register(request));
        }

        @PostMapping("/login")
        public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginDto request) {
                return ResponseEntity.ok(authService.login(request));
        }
}
