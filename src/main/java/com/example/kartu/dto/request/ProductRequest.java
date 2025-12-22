package com.example.kartu.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class ProductRequest {

    private String name;

    private Integer price;

    private String description; // Lebih generik daripada "kuota"

    private Integer stock;

    @NotNull
    private Integer provider;

    @NotNull
    private Integer category;

}
