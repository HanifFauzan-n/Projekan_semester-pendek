package com.example.kartu.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Profile edit. Email is intentionally not editable here: it identifies the account for
 * Google login and OTP, so changing it would need a new verification flow.
 * Password fields are optional; both must be filled to change the password.
 */
@Data
public class UserProfileRequest {
    @NotBlank(message = "Nomor HP wajib diisi.")
    @Pattern(regexp = "^08\\d{7,15}$", message = "Nomor HP harus diawali 08 dan berisi 9-17 digit.")
    private String phoneNumber;

    private String currentPassword;

    @Size(min = 6, message = "Password baru minimal 6 karakter.")
    private String newPassword;
}
