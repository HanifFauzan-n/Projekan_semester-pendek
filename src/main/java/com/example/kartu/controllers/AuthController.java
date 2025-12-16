package com.example.kartu.controllers;

import com.example.kartu.models.User;
import com.example.kartu.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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

        model.addAttribute("user", new User());
        return "login";
    }

    // Menampilkan halaman registrasi
    // Perubahan: Mengganti "/daftar" menjadi "/register"
    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        // Memastikan selalu ada objek user kosong untuk diisi form
        if (!model.containsAttribute("user")) {
            model.addAttribute("user", new User());
        }
        return "registration";
    }

    // Memproses data dari form registrasi
    // Perubahan: Mengganti "/save-akun" menjadi "/register" dengan metode POST
    @PostMapping("/register")
    public String processRegistration(@ModelAttribute("user") User user, RedirectAttributes redirectAttributes) {
        try {
            // Memanggil service untuk melakukan validasi dan penyimpanan
            authService.registerUser(user);
            
            // Mengirim pesan sukses ke halaman login setelah registrasi berhasil
            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please log in.");
            return "redirect:/login";

        } catch (Exception e) {
            // Jika terjadi error validasi dari service
            // Mengirim pesan error dan data yang sudah diisi kembali ke form registrasi
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("user", user);
            return "redirect:/register";
        }
    }
}