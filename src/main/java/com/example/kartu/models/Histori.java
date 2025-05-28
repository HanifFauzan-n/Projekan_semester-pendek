package com.example.kartu.models;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Entity
@Data
public class Histori {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private LocalDateTime tanggal = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "user_pembeli", referencedColumnName = "id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "id_produk", referencedColumnName = "id")
    private Konter konter;

    public void setTanggal(Object cu) {
    }
}
