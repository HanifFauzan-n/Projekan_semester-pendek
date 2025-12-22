package com.example.kartu.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class ResponseUser {
    private String username;
    private String password;

    private String phoneNumber;

    private String danaNumber;

    private Integer balance;

    private String role;

}
