package com.example.kartu.dto.request;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class RequestUser {
    private String username;
    private String password;

    private String phoneNumber;

    private String danaNumber;

    private Integer balance;

}
