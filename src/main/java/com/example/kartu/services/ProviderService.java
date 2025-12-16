package com.example.kartu.services;

import com.example.kartu.models.Provider;
import com.example.kartu.repositories.ProviderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class ProviderService {

    @Autowired
    private ProviderRepository providerRepository;

    // Ambil semua provider
    public List<Provider> findAll() {
        return providerRepository.findAll();
    }

    // Ambil satu provider by ID (opsional, buat jaga-jaga nanti butuh)
    public Provider findById(Integer id) {
        return providerRepository.findById(id).orElse(null);
    }

    // Logika Simpan Provider (Create / Update)
    public void saveProvider(Provider provider, MultipartFile file) throws IOException {
        // Cek apakah ada file gambar yang diupload
        if (file != null && !file.isEmpty()) {
            // Konversi file gambar ke byte[] (BLOB)
            provider.setLogo(file.getBytes());
        } else {
            // Logika Tambahan (Opsional):
            // Jika ini proses UPDATE (id tidak null) dan user TIDAK upload gambar baru,
            // kita harus pertahankan gambar lama agar tidak hilang (jadi null).
            if (provider.getId() != null) {
                Provider oldProvider = providerRepository.findById(provider.getId()).orElse(null);
                if (oldProvider != null) {
                    provider.setLogo(oldProvider.getLogo());
                }
            }
        }
        
        // Simpan ke database
        providerRepository.save(provider);
    }

    // Hapus provider
    public void deleteProvider(Integer id) {
        providerRepository.deleteById(id);
    }
}