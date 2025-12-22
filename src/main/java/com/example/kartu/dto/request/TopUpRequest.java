package com.example.kartu.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TopUpRequest {
    @NotNull
    @Min(10000)
    private Integer amount;
    
    // Gak perlu kirim username/id dari form, ambil dari Principal (Login Session) aja biar aman.
}