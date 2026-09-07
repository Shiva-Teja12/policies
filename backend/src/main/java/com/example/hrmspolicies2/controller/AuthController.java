package com.example.hrmspolicies2.controller;

import com.example.hrmspolicies2.dto.*;
import com.example.hrmspolicies2.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(
        name = "Authentication",
        description = "Employee signup and role-verified login"
)
public class AuthController {

    private final AuthService authService;

    public AuthController(
            AuthService authService
    ) {
        this.authService = authService;
    }

    // =========================================================
    // SIGNUP
    // =========================================================

    @PostMapping("/signup")
    @Operation(
            summary = "Create an Employee account",
            description = "Public signup always assigns EMPLOYEE role"
    )
    public ResponseEntity<AuthResponse> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        authService.signup(request)
                );
    }

    // =========================================================
    // LOGIN
    // =========================================================

    @PostMapping("/login")
    @Operation(
            summary = "Login through the selected role portal"
    )
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(
                authService.login(request)
        );
    }

    // =========================================================
    // FORGOT PASSWORD
    // =========================================================

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @RequestBody ForgotPasswordRequest request
    ) {
        return ResponseEntity.ok(
                authService.forgotPassword(request)
        );
    }

    // =========================================================
    // VALIDATE PASSWORD RESET TOKEN
    // Called when the reset-password page first opens.
    // =========================================================

    @GetMapping("/validate-reset-token")
    public ResponseEntity<String> validateResetToken(
            @RequestParam String token
    ) {
        return ResponseEntity.ok(
                authService.validateResetToken(token)
        );
    }

    // =========================================================
    // RESET PASSWORD
    // =========================================================

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @RequestBody ResetPasswordRequest request
    ) {
        return ResponseEntity.ok(
                authService.resetPassword(request)
        );
    }
}