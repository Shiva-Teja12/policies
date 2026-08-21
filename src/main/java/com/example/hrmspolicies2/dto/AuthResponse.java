package com.example.hrmspolicies2.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {

    private String token;

    private String message;

    private String role;

    private String email;
}