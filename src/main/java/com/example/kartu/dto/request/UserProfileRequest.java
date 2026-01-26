package com.example.kartu.dto.request;
import lombok.Data;

@Data
public class UserProfileRequest {
    private String phoneNumber;
    private String danaNumber;
    
    // Opsional: Ganti Password
    private String currentPassword;
    private String newPassword;
}