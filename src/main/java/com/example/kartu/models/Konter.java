package com.example.kartu.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "Produk")
public class Konter {
    @Id
    private Integer id;
    @Column(name = "nama_kartu")
    private String nama;
    private Integer harga;
    @Column(name = "Pulsa / Kuota")
    private String kuota;
    private Integer stok;
    private String gambar;

    @ManyToOne
    @JoinColumn(name = "kode_kategori",referencedColumnName = "kode")
    private Kategori kodeKategori;


    public void setImagePath(String url) {
    }

}
