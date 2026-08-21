package com.example.hrmspolicies2.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {

    private String name;

    private String email;

    private String password;
}