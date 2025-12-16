package com.example.kartu.models;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Base64; // Import untuk konversi ke HTML nanti
import java.util.List;

@Entity
@Data
public class Provider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    // UBAH DARI STRING KE BYTE[]
    @Lob // Menandakan ini data besar (BLOB)
    @Column(columnDefinition = "MEDIUMBLOB") // Agar muat gambar agak besar
    private byte[] logo;

    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL)
    private List<Product> products;
    
    // Helper method untuk menampilkan gambar di HTML
    public String getLogoBase64() {
        if (logo == null) return null;
        return Base64.getEncoder().encodeToString(logo);
    }
}