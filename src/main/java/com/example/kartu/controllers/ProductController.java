package com.example.kartu.controllers;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.example.kartu.repositories.CategoryRepository;
import com.example.kartu.services.CategoryService;
import com.example.kartu.services.ProductService;
import com.example.kartu.services.ProviderService;
import com.example.kartu.services.TransactionHistoryService;

@Controller
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private TransactionHistoryService transactionService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProviderService providerService;

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
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("providers", providerService.findAll());
        return "add_product";
    }

    @PostMapping("/save-product")
    public String saveProduct(@ModelAttribute("products") Product product, RedirectAttributes redirectAttributes) {
        if (product.getStock() == null || product.getStock() < 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Gagal: Stok tidak boleh negatif!");

            // --- LOGIKA PINTAR DI SINI ---
            if (product.getId() != null) {
                // Kalau punya ID, berarti lagi UPDATE. Balikin ke form update produk tersebut.
                return "redirect:/update-product/" + product.getId();
            } else {
                // Kalau tidak punya ID, berarti lagi ADD NEW. Balikin ke form tambah.
                return "redirect:/add-product";
            }
            // -----------------------------
        }
        try {
            productService.save(product);
            redirectAttributes.addFlashAttribute("successMessage", "Produk berhasil disimpan!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        return "redirect:/home-admin";
    }

    @GetMapping("/update-product/{id}")
    public String showUpdateProductForm(@PathVariable Integer id, Model model) {
        model.addAttribute("products", productService.findById(id));
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("providers", providerService.findAll());
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

    @GetMapping("/purchase-product/{id}")
    public String showPurchasePage(@PathVariable Integer id, Model model) {
        model.addAttribute("products", productService.findById(id));
        return "purchase_confirmation";
    }

    @PostMapping("/purchase")
    public String purchaseProduct(@ModelAttribute("products") Product product, Principal principal,
            RedirectAttributes redirectAttributes) {
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

    @GetMapping("/home-user")
    public String showUserDashboard(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "categoryId", required = false) String categoryId, // Parameter Baru
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {

        int pageSize = 18;

        // Panggil method sakti di Service
        Page<Product> productPage = productService.getProductsWithFilter(keyword, categoryId, page, pageSize);

        // Kirim Data Produk & Halaman
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalItems", productPage.getTotalElements());

        // Kirim State Filter (Biar tidak hilang saat klik page 2)
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);

        // Kirim Daftar Kategori (Untuk Tombol Filter)
        model.addAttribute("categories", categoryService.findAll());

        return "home_user";
    }
}