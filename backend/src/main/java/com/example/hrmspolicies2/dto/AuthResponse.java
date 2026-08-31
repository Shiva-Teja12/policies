package com.example.hrmspolicies2.dto;

import com.example.hrmspolicies2.enums.Role;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {

    private String token;
    private String message;
    private Long userId;
    private String name;
    private String email;
    private Role role;
    private String dashboardPath;
}