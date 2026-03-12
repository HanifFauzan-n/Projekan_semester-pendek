package com.example.kartu.controllers;

import com.example.kartu.models.TopUp;
import com.example.kartu.models.User;
import com.example.kartu.services.TopUpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
public class TopUpController {

    @Autowired
    private TopUpService topUpService;

    // 1. Tampilkan Halaman
    @GetMapping("/topup")
    public String showTopUpPage(Model model, Principal principal) {
        try {
            User user = topUpService.getUser(principal.getName());
            model.addAttribute("user", user);
            return "top_up"; // Pastikan nama file HTML-nya ini
        } catch (Exception e) {
            return "redirect:/home-user";
        }
    }

    // 2. Proses Submit
    @PostMapping("/topup")
    public String processTopUp(@RequestParam("amount") Double amount,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            topUpService.processTopUp(principal.getName(), amount);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Permintaan Top Up diterima! Saldo akan masuk otomatis dalam 3-5 menit.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Gagal: " + e.getMessage());
        }
        return "redirect:/topup";
    }

    @GetMapping("/admin/topups")
    public String showTopUpReport(Model model) {
        // Ambil semua data top up urut dari yang terbaru
        List<TopUp> topUps = topUpService.getAllTopUpsDesc();
        model.addAttribute("topups", topUps);

        return "admin_topup_report"; // Mengarah ke file HTML yang sudah Anda punya
    }

    // 2. Aksi Batalkan Top Up
    @PostMapping("/admin/topups/cancel")
    public String cancelTopUp(@RequestParam("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            topUpService.cancelTopUp(id);
            redirectAttributes.addFlashAttribute("successMessage", "Top Up berhasil dibatalkan (FAILED).");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Gagal membatalkan: " + e.getMessage());
        }
        return "redirect:/admin/topups"; // Refresh halaman
    }
}