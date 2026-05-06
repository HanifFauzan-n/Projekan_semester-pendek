package com.example.kartu.controllers;

import com.example.kartu.dto.request.UserRequest;
import com.example.kartu.models.User;
import com.example.kartu.services.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.web.WebAttributes;
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
            HttpServletRequest request,
            Model model) {

        // Menampilkan pesan error jika login gagal
        if (error != null) {
            // Kita ambil session untuk mengecek error spesifik dari Spring Security
            HttpSession session = request.getSession(false);
            String errorMessage = "Invalid username or password."; // Pesan default

            if (session != null) {
                Exception ex = (Exception) session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
                if (ex != null) {
                    // Jika errornya karena di-banned (LockedException)
                    if (ex instanceof LockedException) {
                        errorMessage = "Your account has been banned by the Administrator.";
                    }
                    // Anda juga bisa menambahkan tipe error lain di sini jika perlu
                }
            }
            model.addAttribute("errorMessage", errorMessage);
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
            @Valid @ModelAttribute("userRequest") UserRequest userRequest,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {

        // 1. Validasi Manual: Cek Password Match
        if (userRequest.getPassword() != null && userRequest.getConfirmPassword() != null) {
            if (!userRequest.getPassword().equals(userRequest.getConfirmPassword())) {
                model.addAttribute("errorMessage", "Password dan Konfirmasi Password tidak cocok!");
                return "registration";
            }
        }

        // 2. Cek Error DTO (Validasi anotasi lain)
        if (result.hasErrors()) {
            String pesanError = result.getAllErrors().get(0).getDefaultMessage();
            model.addAttribute("errorMessage", pesanError);
            return "registration";
        }

        try {
            authService.registerUser(userRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Registrasi Berhasil! Silakan Login.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "registration";
        }
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "forgot_password";
    }

    @PostMapping("/forgot-password")
    public String handlePasswordReset(@RequestParam String username,
            @RequestParam String emergencyNumber,
            @RequestParam String newPassword,
            Model model) {

        boolean isSuccess = authService.resetPasswordWithEmergencyNumber(username, emergencyNumber, newPassword);

        if (isSuccess) {
            model.addAttribute("successMessage", "Password berhasil diperbarui. Silakan login.");
            return "login";
        } else {
            model.addAttribute("errorMessage", "Data tidak cocok. Pastikan Username dan Nomor Darurat benar.");
            return "forgot_password";
        }
    }
}