package com.example.kartu.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "Produk") // Nama tabel di database bisa tetap sama
public class Product {
    @Id
    private Integer id;

    @Column(name = "nama_kartu")
    private String name;

    private Integer price;

    @Column(name = "Pulsa / Kuota")
    private String description; // Lebih generik daripada "kuota"

    private Integer stock;

    @Column(name = "gambar")
    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "kode_kategori")
    private Category category;
}