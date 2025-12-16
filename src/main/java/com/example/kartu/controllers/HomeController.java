package com.example.kartu.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.kartu.services.ProductService;
import com.example.kartu.services.TransactionHistoryService;

@Controller
public class HomeController {

    @Autowired
    private ProductService productService;

    @Autowired
    private TransactionHistoryService transactionService;

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
}