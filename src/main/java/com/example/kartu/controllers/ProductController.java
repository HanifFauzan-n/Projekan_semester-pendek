package com.example.kartu.controllers;

import com.example.kartu.models.Product;
import com.example.kartu.services.ProductService;
import com.example.kartu.services.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class ProductController {

    @Autowired
    private ProductService productService;
    
    @Autowired
    private TransactionService transactionService;

    // == ADMIN ROUTES ==
    @GetMapping("/home-admin")
    public String showAdminDashboard(Model model) {
        model.addAttribute("ktr", productService.findAll());
        return "home_admin";
    }

    @GetMapping("/add-konter")
    public String showAddProductForm(Model model) {
        model.addAttribute("ktr", new Product());
        // Note: You might need to send categories to the form as well
        // model.addAttribute("categories", categoryService.findAll());
        return "addKonter";
    }
    
    @PostMapping("/save-konter")
    public String saveProduct(@ModelAttribute("ktr") Product product) {
        productService.save(product);
        return "redirect:/home-admin";
    }
    
    @GetMapping("/update-konter/{id}")
    public String showUpdateProductForm(@PathVariable Integer id, Model model) {
        model.addAttribute("ktr", productService.findById(id));
        return "updateKonter";
    }
    
    @GetMapping("/delete-konter/{id}")
    public String deleteProduct(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            productService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Product successfully deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/home-admin";
    }

    // == USER ROUTES ==
    @GetMapping("/home-user")
    public String showUserDashboard(Model model) {
        model.addAttribute("ktr", productService.findAll());
        return "home_user";
    }
    
    @GetMapping("/pesan-konter/{id}")
    public String showPurchasePage(@PathVariable Integer id, Model model) {
        model.addAttribute("ktr", productService.findById(id));
        return "pesanKuota";
    }

    @PostMapping("/beli")
    public String purchaseProduct(@ModelAttribute("ktr") Product product, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            // Securely get the username of the logged-in user
            String username = principal.getName(); 
            transactionService.purchaseProduct(product.getId(), username);
            redirectAttributes.addFlashAttribute("successMessage", "Purchase successful!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/home-user";
    }
}