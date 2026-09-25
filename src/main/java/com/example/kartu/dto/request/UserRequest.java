package com.example.kartu.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRequest {
    @NotBlank(message = "Username wajib diisi.")
    @Size(min = 3, max = 50, message = "Username harus 3-50 karakter.")
    private String username;

    @NotBlank(message = "Email wajib diisi.")
    @Email(message = "Format email tidak valid.")
    private String email;

    @NotBlank(message = "Password wajib diisi.")
    @Size(min = 6, message = "Password minimal 6 karakter.")
    private String password;

    @NotBlank(message = "Konfirmasi password wajib diisi.")
    private String confirmPassword;

    @NotBlank(message = "Nomor HP wajib diisi.")
    @Pattern(regexp = "^08\\d{7,15}$", message = "Nomor HP harus diawali 08 dan berisi 9-17 digit.")
    private String phoneNumber;

}
