package com.example.kartu.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserProfileRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 8, message = "Username must be at least 8 characters")
    private String username;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^08\\d{7,15}$", message = "Invalid phone number format (must start with 08 and contain 9-15 digits)")
    private String phoneNumber;

    @NotBlank(message = "Recovery key is required")
    @Size(min = 5, message = "Recovery key must be at least 5 characters")
    @Pattern(regexp = "^[0-9]+$", message = "Recovery key may contain numbers only")
    private String recoveryKey;

}
