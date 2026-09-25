package com.example.kartu.services;

import com.example.kartu.dto.request.UserProfileRequest;
import com.example.kartu.models.User;
import com.example.kartu.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * The logged-in account for both login types: form login authenticates by username,
     * Google login (OAuth2/OIDC) by email.
     */
    public Optional<User> findCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            String email = oauth2User.getAttribute("email");
            if (email != null && !email.isBlank()) {
                return userRepository.findByEmail(email.trim().toLowerCase());
            }
        }
        String name = authentication.getName();
        return userRepository.findByUsername(name).or(() -> userRepository.findByEmail(name));
    }

    /**
     * Updates the phone number and, when both password fields are given, the password.
     * Throws IllegalArgumentException with a user-facing message.
     */
    @Transactional
    public void updateProfile(User user, UserProfileRequest request) {
        String phone = request.getPhoneNumber().trim();
        if (userRepository.existsByPhoneNumberAndUsernameNot(phone, user.getUsername())) {
            throw new IllegalArgumentException("Nomor HP sudah dipakai akun lain.");
        }
        user.setPhoneNumber(phone);

        boolean wantsNewPassword = request.getNewPassword() != null && !request.getNewPassword().isBlank();
        if (wantsNewPassword) {
            if (user.getPassword() == null) {
                throw new IllegalArgumentException("Akun Google tidak memakai password lokal.");
            }
            if (request.getCurrentPassword() == null
                    || !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new IllegalArgumentException("Password lama salah.");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }
        userRepository.save(user);
    }

    public void banUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User data not found"));

        // Ubah status menjadi BANNED
        user.setStatus("BANNED");
        userRepository.save(user);
    }

    public void unbanUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User data not found"));

        // Kembalikan status menjadi ACTIVE
        user.setStatus("ACTIVE");
        userRepository.save(user);
    }
}
