package com.practice.springbootdemo.advance_performance_module.controller;


import com.practice.springbootdemo.advance_performance_module.DTOs.common.ApiResponse;
import com.practice.springbootdemo.advance_performance_module.DTOs.auth.LoginRequest;
import com.practice.springbootdemo.advance_performance_module.security.SecurityUtils;
import com.practice.springbootdemo.advance_performance_module.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User Registration, Login & Session APIs")
public class AuthController {
    private final AuthService authService;
    private final DepartmentService departmentService;

    public AuthController(AuthService authService, DepartmentService departmentService) {
        this.authService = authService;
        this.departmentService = departmentService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register Employee", description = "Register a new employee account")
    public ApiResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.success("Account registered successfully", authService.signup(request));
    }

    @PostMapping("/login")
    @Operation(summary = "User Login", description = "Authenticate credentials and receive JWT Bearer token")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("Login successful", authService.login(request));
    }

    @GetMapping("/departments")
    @Operation(summary = "Public Departments List", description = "List departments available for registration")
    public ApiResponse<List<PublicDepartmentResponse>> departments() {
        return ApiResponse.success(departmentService.listPublic());
    }

    @GetMapping("/me")
    @Operation(summary = "Get Current User", description = "Retrieve profile details for authenticated user")
    public ApiResponse<LoginResponse> me(Authentication authentication) {
        Long userId = SecurityUtils.getUserIdFromAuth(authentication);
        return ApiResponse.success(authService.getCurrentUser(userId));
    }
}
