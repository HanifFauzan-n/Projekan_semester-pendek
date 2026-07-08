package com.example.kartu.controllers;

import com.example.kartu.models.Category;
import com.example.kartu.services.CategoryService; // Import Service

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/categories")
public class CategoryController {

    
    private final CategoryService categoryService; 

    // 1. READ: Tampilkan halaman daftar kategori
    @GetMapping
    public String listCategories(Model model) {
        // Panggil service
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("newCategory", new Category());
        return "manage_categories";
    }

    // 2. CREATE: Simpan kategori baru
    @PostMapping("/save")
    public String saveCategory(@ModelAttribute("newCategory") Category category, RedirectAttributes redirectAttributes) {
        try {
            // Logika "toUpperCase" sudah diurus oleh Service
            categoryService.saveCategory(category);
            
            redirectAttributes.addFlashAttribute("successMessage", "Category saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to save category:" + e.getMessage());
        }
        return "redirect:/categories";
    }

    // 3. DELETE: Hapus kategori
    @GetMapping("/delete/{id}")
    public String deleteCategory(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        try {
            // Service akan melempar error jika kategori masih dipakai produk
            categoryService.deleteCategory(id);
            
            redirectAttributes.addFlashAttribute("successMessage", "Category successfully deleted!");
        } catch (Exception e) {
            // Tangkap pesan error dari Service ("Gagal! Kategori ini sedang digunakan...")
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/categories";
    }
}