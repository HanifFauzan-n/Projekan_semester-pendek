package com.example.kartu.controllers;

import com.example.kartu.models.Provider;
import com.example.kartu.services.ProviderService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/providers")
public class ProviderController {

    private final ProviderService providerService; 

    @GetMapping
    public String listProviders(Model model) {
        model.addAttribute("providers", providerService.findAll());
        model.addAttribute("newProvider", new Provider());
        return "manage_provider";
    }

    @PostMapping("/save")
    public String saveProvider(
            @ModelAttribute("newProvider") Provider provider,
            @RequestParam("image") MultipartFile file, 
            RedirectAttributes redirectAttributes) {

        try {
            // Panggil layanan (Semua validasi terjadi di dalam sini)
            providerService.saveProvider(provider, file);
            
            redirectAttributes.addFlashAttribute("successMessage", "Provider saved successfully!");
            
        } catch (Exception e) {
            // Tangkap Kesalahan dari layanan (misal: "File bukan gambar!")
            redirectAttributes.addFlashAttribute("errorMessage", "Failed: " + e.getMessage());
            
            // Logika Pengalihan Cerdas (Kembali ke Tambah atau Perbarui?)
            if (provider.getId() != null) {
                return "redirect:/update-provider/" + provider.getId(); // Asumsi ada endpoint ini
            } else {
                return "redirect:/admin/providers";
            }
        }

        return "redirect:/admin/providers"; // Halaman daftar provider
    }

    @GetMapping("/delete/{id}")
    public String deleteProvider(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            providerService.deleteProvider(id);
            redirectAttributes.addFlashAttribute("successMessage", "Provider deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/providers";
    }
}
