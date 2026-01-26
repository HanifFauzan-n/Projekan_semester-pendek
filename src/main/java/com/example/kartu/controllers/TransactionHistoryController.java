package com.example.kartu.controllers;

import com.example.kartu.models.Product;
import com.example.kartu.models.User;
import com.example.kartu.services.ProductService;
import com.example.kartu.services.TransactionHistoryService;
import com.example.kartu.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/transaction")
public class TransactionHistoryController {

    @Autowired
    private TransactionHistoryService transactionService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    // 1. Tampilkan Halaman Konfirmasi (Checkout)
    @GetMapping("/confirm/{id}")
    public String showConfirmationPage(@PathVariable("id") Integer productId, Model model, Principal principal) {
        try {
            // Ambil Produk dari Service
            Product product = productService.findById(productId);

            // PERBAIKAN: Ambil User lewat Service (Logic pencarian dipindah ke Service)
            User user = userService.getCurrentUser(principal);

            model.addAttribute("product", product);
            model.addAttribute("user", user);

            return "purchase_confirmation";
        } catch (Exception e) {
            return "redirect:/home-user";
        }
    }

    // 2. Proses Pembelian
    @PostMapping("/process")
    public String processTransaction(@RequestParam("productId") Integer productId,
            @RequestParam(value = "voucherCode", required = false) String code,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            // Eksekusi transaksi (Sudah benar pakai Service)
            transactionService.purchaseProduct(productId, principal.getName(),code);

            redirectAttributes.addFlashAttribute("successMessage", "Transaksi Berhasil!");
            return "redirect:/profile-user";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Gagal: " + e.getMessage());
            return "redirect:/transaction/confirm/" + productId;
        }
    }
}