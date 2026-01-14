package com.example.kartu.controllers;

import com.example.kartu.models.Category;
import com.example.kartu.services.CategoryService; // Import Service
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService; // Ganti Repository dengan Service

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
            
            redirectAttributes.addFlashAttribute("successMessage", "Kategori berhasil disimpan!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Gagal menyimpan kategori: " + e.getMessage());
        }
        return "redirect:/categories";
    }

    // 3. DELETE: Hapus kategori
    @GetMapping("/delete/{id}")
    public String deleteCategory(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        try {
            // Service akan melempar error jika kategori masih dipakai produk
            categoryService.deleteCategory(id);
            
            redirectAttributes.addFlashAttribute("successMessage", "Kategori berhasil dihapus!");
        } catch (Exception e) {
            // Tangkap pesan error dari Service ("Gagal! Kategori ini sedang digunakan...")
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/categories";
    }
}