package com.example.kartu.controllers;

import java.security.Principal;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.kartu.models.Product;
import com.example.kartu.services.CategoryService;
import com.example.kartu.services.ProductService;
import com.example.kartu.services.ProviderService;
import com.example.kartu.services.TransactionHistoryService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ProductController {

    
    private final ProductService productService;

    
    private final TransactionHistoryService transactionService;

    
    private final CategoryService categoryService;

    
    private final ProviderService providerService;

    // == ROUTE ADMIN ==
    @GetMapping("/home-admin")
    public String showAdminDashboard(Model model) {
        model.addAttribute("products", productService.findAll());
        return "home_admin";
    }

    @GetMapping("/add-product")
    public String showAddProductForm(Model model) {
        model.addAttribute("products", new Product());
        // Catatan: Anda mungkin juga perlu mengirim kategori ke formulir
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("providers", providerService.findAll());
        return "add_product";
    }

    @PostMapping("/save-product")
    public String saveProduct(@ModelAttribute("products") Product product, RedirectAttributes redirectAttributes) {
        if (product.getStock() == null || product.getStock() < 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed: Stock cannot be negative!");

            // --- LOGIKA PINTAR DI SINI ---
            if (product.getId() != null) {
                // Kalau punya ID, berarti sedang MEMPERBARUI. Kembalikan ke formulir pembaruan produk tersebut.
                return "redirect:/update-product/" + product.getId();
            } else {
                // Kalau tidak punya ID, berarti sedang MENAMBAH DATA BARU. Kembalikan ke formulir tambah.
                return "redirect:/add-product";
            }
            // -----------------------------
        }
        try {
            productService.save(product);
            redirectAttributes.addFlashAttribute("successMessage", "Product saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        return "redirect:/home-admin";
    }

    @GetMapping("/update-product/{id}")
    public String showUpdateProductForm(@PathVariable Integer id, Model model) {
        model.addAttribute("products", productService.findById(id));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("providers", providerService.findAll());
        return "update_product";
    }

    @GetMapping("/delete-product/{id}")
    public String deleteProduct(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            productService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Product deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/home-admin";
    }

    @GetMapping("/purchase-product/{id}")
    public String showPurchasePage(@PathVariable Integer id, Model model) {
        model.addAttribute("products", productService.findById(id));
        return "purchase_confirmation";
    }

    @PostMapping("/purchase")
    public String purchaseProduct(@ModelAttribute("products") Product product,
            @RequestParam(value = "voucherCode", required = false) String code, Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            // Ambil username pengguna yang sedang login secara aman
            String username = principal.getName();
            transactionService.purchaseProduct(product.getId(), username, code);
            redirectAttributes.addFlashAttribute("successMessage", "Purchase successful!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/home-user";
    }

    @GetMapping("/home-user")
    public String showUserDashboard(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "categoryId", required = false) String categoryId, // Parameter Baru
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {

        int pageSize = 18;

        // Panggil metode di layanan
        Page<Product> productPage = productService.getProductsWithFilter(keyword, categoryId, page, pageSize);

        // Kirim Data Produk & Halaman
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalItems", productPage.getTotalElements());

        // Kirim Status Filter (Agar tidak hilang saat klik halaman 2)
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);

        // Kirim Daftar Kategori (Untuk Tombol Filter/Penyaringan)
        model.addAttribute("categories", categoryService.findAll());

        return "home_user";
    }
}
