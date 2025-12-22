package com.example.kartu.dto.request;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class RequestProduct {

    private String name;

    private Integer price;

    private String description; // Lebih generik daripada "kuota"

    private Integer stock;

    private Integer provider;

    private Integer category;

}
