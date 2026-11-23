package com.ilham.portfolio.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CVRequest(

        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name is too long")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Please enter a valid email address")
        @Size(max = 254)
        String email,

        @Size(max = 120)
        String company,

        @Size(max = 1000)
        String message

) {}

