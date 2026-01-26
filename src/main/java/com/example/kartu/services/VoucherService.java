package com.example.kartu.services;

import com.example.kartu.models.Voucher;
import com.example.kartu.repositories.VoucherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VoucherService {

    @Autowired
    private VoucherRepository voucherRepository;

    // Ambil semua voucher untuk ditampilkan di tabel
    public List<Voucher> getAllVouchers() {
        return voucherRepository.findAll();
    }

    // Simpan atau Update Voucher
    public void saveVoucher(Voucher voucher) {
        // Uppercase kode biar seragam (misal: "diskon10" jadi "DISKON10")
        voucher.setCode(voucher.getCode().toUpperCase());
        voucherRepository.save(voucher);
    }

    // Hapus Voucher
    public void deleteVoucher(Integer id) {
        voucherRepository.deleteById(id);
    }
    
    // Validasi Voucher (dipakai saat user beli)
    // ... logic validasi yang sudah kita bahas sebelumnya ...
}