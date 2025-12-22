package com.example.kartu.dto.response;

import com.example.kartu.models.Category;
import com.example.kartu.models.Provider;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class ResponseProduct {
    private String name;

    private Integer price;

    private String description; // Lebih generik daripada "kuota"

    private Integer stock;

    private Provider provider;

    private Category category;

}
