package com.example.kartu.controllers;

import com.example.kartu.models.Provider;
import com.example.kartu.services.ProviderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequestMapping("/admin/providers")
public class ProviderController {

    @Autowired
    private ProviderService providerService; 

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
            // Panggil Service (Semua validasi terjadi di dalam sini)
            providerService.saveProvider(provider, file);
            
            redirectAttributes.addFlashAttribute("successMessage", "Provider berhasil disimpan!");
            
        } catch (Exception e) {
            // Tangkap Error dari Service (misal: "File bukan gambar!")
            redirectAttributes.addFlashAttribute("errorMessage", "Gagal: " + e.getMessage());
            
            // Logika Smart Redirect (Balik ke Add atau Update?)
            if (provider.getId() != null) {
                return "redirect:/update-provider/" + provider.getId(); // Asumsi ada endpoint ini
            } else {
                return "redirect:/admin/providers";
            }
        }

        return "redirect:/admin/providers"; // Halaman list provider
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