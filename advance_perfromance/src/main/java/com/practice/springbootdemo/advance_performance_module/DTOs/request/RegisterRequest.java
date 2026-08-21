package com.practice.springbootdemo.advance_performance_module.DTOs.request;


import com.practice.springbootdemo.advance_performance_module.entities.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank
    private String name;

    @NotBlank @Email
    private String email;

    @NotBlank
    private String password;

    @NotNull
    private Role role;

    private String department;
}
