package com.example.kartu.services;

import com.example.kartu.models.User;
import com.example.kartu.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

/**
 * SKPL-F03: maps a Google identity to a local account. Shared by the OAuth2 and OIDC user
 * services (Google uses OIDC when the "openid" scope is requested).
 *
 * - Email already registered: the account is linked (Google has verified the address), so no
 *   duplicate account is created. A banned account stays banned and the login is refused.
 * - New email: an account is created with role USER, balance 0 and no local password.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GoogleAccountService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public User upsert(String email, String name, String sub, String picture) {
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("email_missing"),
                    "Akun Google Anda tidak memiliki alamat email publik.");
        }
        email = email.trim().toLowerCase();

        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            if ("BANNED".equals(user.getStatus())) {
                throw new OAuth2AuthenticationException(new OAuth2Error("account_banned"),
                        "Akun ini diblokir. Hubungi admin Zelatan Cell.");
            }
            user.setEmailVerified(true);
            if ("PENDING_VERIFICATION".equals(user.getStatus())) {
                user.setStatus("ACTIVE");
            }
            if (user.getProviderId() == null) {
                user.setProviderId(sub);
            }
            if (user.getProfilePictureUrl() == null && picture != null) {
                user.setProfilePictureUrl(picture);
            }
            log.info("Login via Google untuk akun terdaftar: {} ({})", user.getUsername(), email);
            return userRepository.save(user);
        }

        User created = new User();
        created.setUsername(uniqueUsername(name, email));
        created.setEmail(email);
        created.setEmailVerified(true);
        created.setStatus("ACTIVE");
        created.setRole("ROLE_USER");
        created.setBalance(0);
        created.setAuthProvider("GOOGLE");
        created.setProviderId(sub);
        created.setProfilePictureUrl(picture);
        User saved = userRepository.save(created);
        log.info("Akun baru dibuat otomatis via Google: {} ({})", saved.getUsername(), email);

        EmailService.afterCommit(() -> emailService.sendWelcome(saved.getEmail(), saved.getUsername()));
        return saved;
    }

    private String uniqueUsername(String name, String email) {
        String base = (name != null && !name.isBlank() ? name : email.split("@")[0])
                .replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        if (base.length() < 3) {
            base = "user" + (100 + random.nextInt(900));
        }
        String username = base;
        int counter = 1;
        while (userRepository.findByUsername(username).isPresent()) {
            username = base + counter++;
        }
        return username;
    }
}
