package com.example.hrmspolicies2.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 120, message = "Full name cannot exceed 120 characters")
    private String name;

    @NotBlank(message = "Company email is required")
    @Email(message = "Enter a valid company email")
    @Size(max = 190, message = "Email cannot exceed 190 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must contain 8 to 72 characters")
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;
}