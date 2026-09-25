package com.example.kartu.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Base64; // Impor untuk konversi ke HTML nanti
import java.util.List;

@Entity
@Data
public class Provider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    // Not serialized with products or flash sales: every product would repeat the image.
    // GET /api/providers sends it once per provider as logoBase64.
    @JsonIgnore
    @Column(name = "logo", columnDefinition = "bytea")
    private byte[] logo;

    // Back-reference only; serializing it loops Provider -> Product -> Provider.
    @JsonIgnore
    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Product> products;

    // Metode pembantu untuk menampilkan gambar di HTML
    @JsonIgnore
    public String getLogoBase64() {
        if (logo == null) return null;
        return Base64.getEncoder().encodeToString(logo);
    }
}
