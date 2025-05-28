package com.example.kartu.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;

@Entity
@Data
@AllArgsConstructor
public class Kategori {
    @Id
    private Integer kode;

    private String tipe;
    public Kategori(){

    }
}
