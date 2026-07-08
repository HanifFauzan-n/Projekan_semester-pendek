package com.example.kartu.controllers;

import com.example.kartu.dto.request.UserProfileRequest;
import com.example.kartu.models.User;
import com.example.kartu.services.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    // 1. Tampilkan Form Edit
    @GetMapping("/edit")
    public String showEditProfileForm(Model model, Principal principal) {
        User user = userService.getCurrentUser(principal);
        
        // Siapkan DTO dengan data lama biar form terisi otomatis
        UserProfileRequest request = new UserProfileRequest();
        request.setUsername(user.getUsername());
        request.setPhoneNumber(user.getPhoneNumber());
        
        model.addAttribute("profileRequest", request);
        model.addAttribute("user", user); // Untuk menampilkan foto/nama di navbar/header
        
        return "edit_profile"; // Nama file HTML nanti
    }

    // 2. Proses Update Data
    @PostMapping("/update-profile")
    public String processUpdateProfile(@ModelAttribute UserProfileRequest request,
                                       Principal principal,
                                       RedirectAttributes redirectAttributes) {
        try {
            userService.updateUserProfile(principal.getName(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
            return "redirect:/profile-user";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/user/edit"; // Balik ke form edit jika gagal
        }
    }
}