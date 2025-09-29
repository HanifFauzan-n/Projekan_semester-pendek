package com.example.kartu.controllers;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.kartu.models.Product;
import com.example.kartu.services.ProductService;
import com.example.kartu.services.TransactionService;

@Controller
public class ProductController {

    @Autowired
    private ProductService productService;
    
    @Autowired
    private TransactionService transactionService;

    // == ADMIN ROUTES ==
    @GetMapping("/home-admin")
    public String showAdminDashboard(Model model) {
        model.addAttribute("products", productService.findAll());
        return "home_admin";
    }

    @GetMapping("/add-product")
    public String showAddProductForm(Model model) {
        model.addAttribute("products", new Product());
        // Note: You might need to send categories to the form as well
        // model.addAttribute("categories", categoryService.findAll());
        return "add_product";
    }
    
    @PostMapping("/save-product")
    public String saveProduct(@ModelAttribute("products") Product product) {
        productService.save(product);
        return "redirect:/home-admin";
    }
    
    @GetMapping("/update-product/{id}")
    public String showUpdateProductForm(@PathVariable Integer id, Model model) {
        model.addAttribute("products", productService.findById(id));
        return "update_product";
    }
    
    @GetMapping("/delete-product/{id}")
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
        model.addAttribute("products", productService.findAll());
        return "home_user";
    }
    
    @GetMapping("/purchase-product/{id}")
    public String showPurchasePage(@PathVariable Integer id, Model model) {
        model.addAttribute("products", productService.findById(id));
        return "purchase_confirmation";
    }

    @PostMapping("/purchase")
    public String purchaseProduct(@ModelAttribute("products") Product product, Principal principal, RedirectAttributes redirectAttributes) {
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