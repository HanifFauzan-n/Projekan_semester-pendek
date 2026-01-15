package com.example.kartu.controllers;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.kartu.enums.TransactionStatus;
import com.example.kartu.models.TopUp;
import com.example.kartu.models.TransactionHistory;
import com.example.kartu.models.User;
import com.example.kartu.repositories.TopUpRepository;
import com.example.kartu.repositories.TransactionHistoryRepository;
import com.example.kartu.repositories.UserRepository;
import com.example.kartu.services.ProductService;
import com.example.kartu.services.TransactionHistoryService;

import java.util.List;

@Controller
public class HomeController {

    @Autowired
    private ProductService productService;

    @Autowired
    private TransactionHistoryService transactionService;

    @Autowired
    private TopUpRepository topUpRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionHistoryRepository transactionHistoryRepository;

    @GetMapping("/")
    public String showPublicHomePage(Model model) {
        model.addAttribute("products", productService.findAll());
        return "home";
    }

    @GetMapping("/home-redirect")
    public String homeRedirect(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            boolean isAdmin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
            if (isAdmin) {
                return "redirect:/home-admin";
            } else {
                return "redirect:/home-user";
            }
        }
        return "redirect:/login";
    }

    @GetMapping("/profile-admin")
    public String showAdminProfile(Model model) {
        // Ambil data statistik dari Service
        long totalProducts = productService.countTotalProducts();
        long totalRevenue = transactionService.calculateTotalRevenue();

        // Kirim ke HTML
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("totalRevenue", totalRevenue);
        
        return "profile_admin";
    }

    @GetMapping("/profile-user")
    public String showUserProfile(Model model, Principal principal) {
        // 1. Ambil username yang sedang login
        String username = principal.getName();

        // 2. Cari User lengkap dari database (agar saldonya update)
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 3. Ambil riwayat transaksi user tersebut
        List<TransactionHistory> history = transactionHistoryRepository.findByUserId(user.getId());

        List<TopUp> topups = topUpRepository.findByUserIdOrderByDateDesc(user.getId());
        model.addAttribute("topups", topups);

        // 4. Hitung Statistik (Opsional: Hitung hanya yang SUKSES untuk Total Spending)
        long totalTransactions = history.size();
        
        long totalSpending = history.stream()
                .filter(tx -> tx.getStatus() == TransactionStatus.SUCCESS) // Hanya hitung yang sukses
                .mapToLong(tx -> tx.getProduct().getPrice())
                .sum();

        // 5. Kirim data ke HTML
        model.addAttribute("user", user);
        model.addAttribute("history", history);
        model.addAttribute("totalTransactions", totalTransactions);
        model.addAttribute("totalSpending", totalSpending);

        return "profile_user";
    }
}