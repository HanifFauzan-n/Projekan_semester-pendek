package com.example.kartu.services;

import com.example.kartu.models.Provider;
import com.example.kartu.repositories.ProductRepository;
import com.example.kartu.repositories.ProviderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Service
public class ProviderService {

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private ProductRepository productRepository;

    // Ambil semua provider
    public List<Provider> findAll() {
        return providerRepository.findAll();
    }

    // Ambil satu provider by ID (opsional, buat jaga-jaga nanti butuh)
    public Provider findById(Integer id) {
        return providerRepository.findById(id).orElse(null);
    }

    public void saveProvider(Provider provider, MultipartFile file) throws Exception {

        // 1. Cek Apakah Ada File yang Diupload?
        if (!file.isEmpty()) {

            // A. Validasi Tipe File (MIME Type)
            String contentType = file.getContentType();
            List<String> allowedTypes = Arrays.asList("image/png", "image/jpeg", "image/jpg");

            if (!allowedTypes.contains(contentType)) {
                throw new Exception("File harus berupa gambar (PNG/JPG)!");
            }

            // B. Validasi Ukuran File (Max 1MB)
            if (file.getSize() > 1024 * 1024) {
                throw new Exception("Ukuran gambar maksimal 1MB!");
            }

            // C. Konversi ke Base64
            provider.setLogo(file.getBytes()); // Simpan string panjang ini ke entity

        } else {
            // Jika File Kosong...

            // Kasus: Tambah Baru (Wajib ada logo)
            if (provider.getId() == null) {
                throw new Exception("Wajib upload logo provider untuk data baru!");
            }

            // Kasus: Update (Edit)
            // Jika user tidak upload gambar baru, kita HARUS pertahankan gambar lama.
            // Ambil data lama dari database:
            Provider oldData = providerRepository.findById(provider.getId())
                    .orElseThrow(() -> new Exception("Provider tidak ditemukan"));

            provider.setLogo(oldData.getLogo()); // Pakai logo lama
        }

        // 2. Simpan ke Database
        providerRepository.save(provider);
    }

    // Hapus provider
    public void deleteProvider(Integer id) throws Exception {
        long productCount = productRepository.countByProviderId(id);

        if (productCount > 0) {
            // 2. Jika ada, LEMPAR ERROR (Jangan dihapus!)
            throw new Exception(
                    "Gagal menghapus! Provider ini masih digunakan oleh " + productCount + " produk aktif.");
        }

        // 3. Jika aman (0 produk), baru hapus
        providerRepository.deleteById(id);
    }
}