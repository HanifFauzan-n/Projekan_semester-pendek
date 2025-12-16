package com.example.kartu.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "product") // Nama tabel di database bisa tetap sama
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "product_name")
    private String name;

    @Column(name = "price")
    private Integer price;

    @Column(name = "description")
    private String description; // Lebih generik daripada "kuota"

    @Column(name = "stock")
    private Integer stock;

    @ManyToOne
    @JoinColumn(name = "provider_id")
    private Provider provider;


    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
}