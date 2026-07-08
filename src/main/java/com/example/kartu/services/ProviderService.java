package com.example.kartu.services;

import com.example.kartu.models.Provider;
import com.example.kartu.repositories.ProductRepository;
import com.example.kartu.repositories.ProviderRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProviderService {

    private final ProviderRepository providerRepository;

    private final ProductRepository productRepository;

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
                throw new Exception("File must be an image (PNG/JPG)!");
            }

            // B. Validasi Ukuran File (Max 1MB)
            if (file.getSize() > 1024 * 1024) {
                throw new Exception("Maximum image size is 1MB!");
            }

            // C. Konversi ke Base64
            provider.setLogo(file.getBytes()); // Simpan string panjang ini ke entity

        } else {
            // Jika File Kosong...

            // Kasus: Tambah Baru (Wajib ada logo)
            if (provider.getId() == null) {
                throw new Exception("Uploading the provider logo is required for new data!");
            }

            // Kasus: Update (Edit)
            // Jika user tidak upload gambar baru, kita HARUS pertahankan gambar lama.
            // Ambil data lama dari database:
            Provider oldData = providerRepository.findById(provider.getId())
                    .orElseThrow(() -> new Exception("Provider not found"));

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
            throw new Exception("Operation failed! This provider is currently in use by " + productCount + " active products.");
        }

        // 3. Jika aman (0 produk), baru hapus
        providerRepository.deleteById(id);
    }
}