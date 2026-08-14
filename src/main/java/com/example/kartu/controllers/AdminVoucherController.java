package com.example.kartu.controllers;

import com.example.kartu.models.Voucher;
import com.example.kartu.services.VoucherService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/vouchers")
public class AdminVoucherController {

    
    private final VoucherService voucherService;

    // 1. Tampilkan Halaman Daftar Voucher
    @GetMapping
    public String listVouchers(Model model) {
        model.addAttribute("vouchers", voucherService.getAllVouchers());
        model.addAttribute("newVoucher", new Voucher()); // Untuk form modal
        return "admin_vouchers"; // Nama file HTML
    }

    // 2. Proses Simpan Voucher Baru
    @PostMapping("/save")
    public String saveVoucher(@ModelAttribute Voucher voucher, RedirectAttributes redirectAttributes) {
        try {
            voucherService.saveVoucher(voucher);
            redirectAttributes.addFlashAttribute("successMessage", "Voucher saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to save voucher: the code may already be in use.");
        }
        return "redirect:/admin/vouchers";
    }

    // 3. Hapus Voucher
    @GetMapping("/delete/{id}")
    public String deleteVoucher(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        voucherService.deleteVoucher(id);
        redirectAttributes.addFlashAttribute("successMessage", "Voucher deleted successfully.");
        return "redirect:/admin/vouchers";
    }
}
