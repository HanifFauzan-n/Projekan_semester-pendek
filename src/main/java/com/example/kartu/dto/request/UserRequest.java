package com.example.kartu.dto.request;

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
    @NotBlank(message = "Username wajib diisi")
    @Size(min = 8, message = "Username minimal 8 karakter")
    private String username;

    @NotBlank(message = "Password wajib diisi")
    @Size(min = 8, message = "Password minimal 8 karakter")
    private String password;

    @NotBlank(message = "Nomor HP wajib diisi")
    @Pattern(regexp = "^08\\d{10,13}$", message = "Format HP salah (harus 08xxx, 12-15 digit)")
    private String phoneNumber;

    @NotBlank(message = "Nomor DANA wajib diisi")
    @Pattern(regexp = "^08\\d{10,13}$", message = "Format DANA salah")
    private String danaNumber;

    private Integer balance;

}
