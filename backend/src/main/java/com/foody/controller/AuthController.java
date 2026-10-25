package com.foody.controller;

import com.foody.dto.ForgotPasswordRequest;
import com.foody.dto.LoginRequest;
import com.foody.dto.RegisterRequest;
import com.foody.dto.ResetPasswordRequest;
import com.foody.service.PasswordResetService;
import com.foody.service.AuthService;
import com.foody.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(UserService userService, AuthService authService,
                          PasswordResetService passwordResetService) {
        this.userService = userService;
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(Map.of("message", passwordResetService.requestReset(request.getEmail())));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(Map.of("message",
                passwordResetService.resetPassword(request.getToken(), request.getNewPassword())));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        String msg = userService.registerUser(request);
        return ResponseEntity.ok(Map.of("message", msg));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(authService.login(request));
    }
}
