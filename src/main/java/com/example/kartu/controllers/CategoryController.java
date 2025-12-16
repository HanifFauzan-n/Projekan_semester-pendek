package com.example.kartu.controllers;

import com.example.kartu.models.Category;
import com.example.kartu.repositories.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/categories") // Prefix URL agar rapi
public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    // 1. READ: Tampilkan halaman daftar kategori
    @GetMapping
    public String listCategories(Model model) {
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("newCategory", new Category()); // Untuk form tambah
        return "manage_categories"; // Mengarah ke template HTML
    }

    // 2. CREATE: Simpan kategori baru
    @PostMapping("/save")
    public String saveCategory(@ModelAttribute("newCategory") Category category, RedirectAttributes redirectAttributes) {
        try {
            category.setType(category.getType().toUpperCase());
            categoryRepository.save(category);
            redirectAttributes.addFlashAttribute("successMessage", "Kategori berhasil disimpan!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Gagal menyimpan kategori: " + e.getMessage());
        }
        return "redirect:/categories";
    }

    // 3. DELETE: Hapus kategori
    @GetMapping("/delete/{id}")
    public String deleteCategory(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            categoryRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Kategori berhasil dihapus!");
        } catch (Exception e) {
            // Error biasanya terjadi jika kategori masih dipakai oleh Produk
            redirectAttributes.addFlashAttribute("errorMessage", "Tidak bisa menghapus kategori yang sedang digunakan oleh produk.");
        }
        return "redirect:/categories";
    }
    
    // Opsional: Edit (Bisa digabung dengan save jika ID-nya ada)
}