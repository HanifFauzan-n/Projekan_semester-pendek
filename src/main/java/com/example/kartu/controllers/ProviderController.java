package com.example.kartu.controllers;

import com.example.kartu.models.Provider;
import com.example.kartu.services.ProviderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequestMapping("/admin/providers")
public class ProviderController {

    @Autowired
    private ProviderService providerService; // Sekarang pakai Service, bukan Repository

    @GetMapping
    public String listProviders(Model model) {
        model.addAttribute("providers", providerService.findAll());
        model.addAttribute("newProvider", new Provider());
        return "manage_provider";
    }

    @PostMapping("/save")
    public String saveProvider(@ModelAttribute("newProvider") Provider provider,
            @RequestParam("image") MultipartFile multipartFile,
            RedirectAttributes redirectAttributes) {
        try {
            // Panggil Service untuk menangani logika penyimpanan
            providerService.saveProvider(provider, multipartFile);

            redirectAttributes.addFlashAttribute("successMessage", "Provider berhasil disimpan!");
        } catch (IOException e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Gagal mengupload gambar: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Terjadi kesalahan: " + e.getMessage());
        }

        return "redirect:/admin/providers";
    }

    @GetMapping("/delete/{id}")
    public String deleteProvider(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            providerService.deleteProvider(id);
            redirectAttributes.addFlashAttribute("successMessage", "Provider berhasil dihapus!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/providers";
    }
}