package com.example.kartu.services;

import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.kartu.dto.request.UserRequest;
import com.example.kartu.models.User;
import com.example.kartu.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    public void registerUser(UserRequest requestUser) throws Exception {

        // 1. Username Validation
        if (userRepository.findByUsername(requestUser.getUsername()).isPresent()) {
            throw new Exception("Username is already taken, please choose another.");
        }

        String usernameRegex = "^(?=(?:.*[a-zA-Z]){3,}).+$";
        if (!requestUser.getUsername().matches(usernameRegex)) {
            throw new Exception("Username must contain at least 3 letters!");
        }

        if (userRepository.findByPhoneNumber(requestUser.getPhoneNumber()).isPresent()) {
            throw new Exception("Phone Number is already taken, please choose another.");
        }

        User user = new User();
        user.setUsername(requestUser.getUsername());
        user.setPassword(passwordEncoder.encode(requestUser.getPassword())); // Enkripsi password
        user.setPhoneNumber(requestUser.getPhoneNumber());
        user.setRecoveryKey(requestUser.getRecoveryKey());

        // Set Default Role (Hardcode biar aman)
        user.setRole("ROLE_USER");

        userRepository.save(user);
    }

    public boolean resetPasswordWithRecoveryKey(String username, String recoveryKey, String newPassword) {
        Optional<User> userOpt = userRepository.findByUsernameAndRecoveryKey(username, recoveryKey);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return true;
        }
        return false;
    }
}