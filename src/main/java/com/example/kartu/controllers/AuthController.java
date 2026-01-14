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

    @PostMapping("/register")
    public String processRegistration(
            @Valid @ModelAttribute("userRequest") UserRequest userRequest, // Tetep pake DTO
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) { // Tambah Model di sini

        // 1. Cek Error DTO
        if (result.hasErrors()) {
            // == TRIK RAHASIA ==
            // Ambil pesan error PERTAMA aja dari list error DTO
            String pesanError = result.getAllErrors().get(0).getDefaultMessage();

            // Masukin ke variabel 'errorMessage' biar HTML lo nampilin kayak biasa
            model.addAttribute("errorMessage", pesanError);

            return "registration";
        }

        try {
            authService.registerUser(userRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Registration successful!");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "registration";
        }
    }
}