package com.example.kartu.dto.request;

import lombok.Data;

@Data
public class UserProfileRequest {
    private String username;
    private String phoneNumber;
    private String recoveryKey;

}