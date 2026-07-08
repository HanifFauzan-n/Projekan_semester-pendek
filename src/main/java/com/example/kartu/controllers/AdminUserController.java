package com.example.kartu.controllers;

import com.example.kartu.models.User;
import com.example.kartu.services.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/users") // Berubah dari /admin/pengguna menjadi /admin/users
@PreAuthorize("hasAuthority('ADMIN') or hasRole('ADMIN')")
public class AdminUserController {

    
    private final UserService userService;

    // Menampilkan daftar user
    @GetMapping
    public String listUsers(Model model) {
        List<User> userList = userService.getAllUsers();
        model.addAttribute("users", userList);

        // Tetap menggunakan nama file HTML yang sama seperti kesepakatan sebelumnya
        return "admin_user_management";
    }

    // Menampilkan detail user
    @GetMapping("/detail/{id}")
    public String userDetails(@PathVariable Integer id, Model model) {
        try {
            User userDetail = userService.getUserDetailsById(id);
            model.addAttribute("user", userDetail);

            // Asumsi jika nanti dibuatkan file detail: letakkan langsung di root templates
            return "admin_user_detail";

        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", "User data not found!");
            return "redirect:/admin/users"; // Redirect disesuaikan ke /admin/users
        }
    }

    // Blokir user
    @PostMapping("/ban/{id}")
    public String banUser(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            userService.banUser(id);
            // Flash attribute untuk memunculkan notifikasi sukses di HTML
            redirectAttributes.addFlashAttribute("successMessage", "User account has been successfully banned!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // Aktifkan kembali user
    @PostMapping("/unban/{id}")
    public String unbanUser(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            userService.unbanUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "User account has been successfully reactivated!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }
}