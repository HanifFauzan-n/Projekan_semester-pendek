package com.example.kartu.controllers;

import com.example.kartu.dto.request.UserRequest;
import com.example.kartu.models.User;
import com.example.kartu.services.AuthService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    @Autowired
    private AuthService authService;

    // Menampilkan halaman login
    @GetMapping("/login")
    public String showLoginPage(@RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {

        // Menampilkan pesan error jika login gagal
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid username or password.");
        }
        // Menampilkan pesan sukses setelah logout
        if (logout != null) {
            model.addAttribute("successMessage", "You have been logged out successfully.");
        }

        model.addAttribute("userRequest", new User());
        return "login";
    }

    // Menampilkan halaman registrasi
    // Perubahan: Mengganti "/daftar" menjadi "/register"
    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        if (!model.containsAttribute("userRequest")) {
            model.addAttribute("userRequest", new UserRequest()); // Ganti User() jadi UserRequest()
        }
        return "registration";
    }

    // Memproses data dari form registrasi
    // Perubahan: Mengganti "/save-akun" menjadi "/register" dengan metode POST
    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("userRequest") UserRequest requestUser,
            BindingResult result, // Buat nangkep error validasi
            RedirectAttributes redirectAttributes,
            Model model) {

        // 1. Cek Validasi DTO (Otomatis dari anotasi @Size, @Pattern tadi)
        if (result.hasErrors()) {
            // Kalau error, balikin ke halaman register beserta pesan errornya
            return "registration";
        }

        try {
            // 2. Kirim DTO ke Service
            authService.registerUser(requestUser);

            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please log in.");
            return "redirect:/login";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/register";
        }
    }
}