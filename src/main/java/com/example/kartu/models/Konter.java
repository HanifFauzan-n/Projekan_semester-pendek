package com.example.kartu.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
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

    public Konter() {

    }

    public Konter(Integer id, String nama, Integer harga, String kuota, Integer stok) {
        this.id = id;
        this.nama = nama;
        this.harga = harga;
        this.kuota = kuota;
        this.stok = stok;
    }

    public void setImagePath(String url) {
    }

}
