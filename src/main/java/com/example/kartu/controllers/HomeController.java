package com.example.kartu.controllers;

import com.example.kartu.models.User;
import com.example.kartu.services.ProductService;
import com.example.kartu.services.TopUpService;
import com.example.kartu.services.TransactionHistoryService;
import com.example.kartu.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class HomeController {

    // Inject SEMUA Service yang dibutuhkan
    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    @Autowired
    private TransactionHistoryService transactionHistoryService;

    @Autowired
    private TopUpService topUpService;

    // 1. Halaman Depan (Public)
    @GetMapping("/")
    public String showPublicHomePage(Model model) {
        model.addAttribute("products", productService.findAll());
        return "home";
    }

    // 2. Logic Redirect Login
    @GetMapping("/home-redirect")
    public String homeRedirect(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            boolean isAdmin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
            return isAdmin ? "redirect:/home-admin" : "redirect:/home-user";
        }
        return "redirect:/login";
    }

    // 3. Profile Admin
    @GetMapping("/profile-admin")
    public String showAdminProfile(Model model) {
        model.addAttribute("totalProducts", productService.countTotalProducts());
        model.addAttribute("totalRevenue", transactionHistoryService.calculateTotalRevenue());
        return "profile_admin";
    }

    // 4. Profile User (SEKARANG JAUH LEBIH BERSIH)
    @GetMapping("/profile-user")
    public String showUserProfile(Model model, Principal principal) {
        // A. Ambil User dari Service
        User user = userService.getCurrentUser(principal);

        // B. Ambil Data Transaksi & Topup dari Service masing-masing
        var history = transactionHistoryService.getTransactionHistoryByUser(user);
        var topups = topUpService.getTopUpHistoryByUser(user);

        // C. Hitung Statistik Sederhana (Bisa juga dipindah ke Service kalau mau lebih rapi lagi)
        long totalTransactions = history.size();
        long totalSpending = history.stream()
                .filter(tx -> "SUCCESS".equals(tx.getStatus().name()))
                .mapToLong(tx -> tx.getProduct().getPrice())
                .sum();

        // D. Kirim ke HTML
        model.addAttribute("user", user);
        model.addAttribute("history", history);
        model.addAttribute("topups", topups);
        model.addAttribute("totalTransactions", totalTransactions);
        model.addAttribute("totalSpending", totalSpending);

        return "profile_user";
    }
}