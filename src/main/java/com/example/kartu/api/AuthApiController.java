package com.example.kartu.api;

import com.example.kartu.dto.request.LoginRequest;
import com.example.kartu.dto.request.UserRequest;
import com.example.kartu.models.User;
import com.example.kartu.repositories.UserRepository;
import com.example.kartu.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final AuthService authService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/csrf")
    public ResponseEntity<?> getCsrfToken(jakarta.servlet.http.HttpServletRequest request) {
        org.springframework.security.web.csrf.CsrfToken csrfToken = 
            (org.springframework.security.web.csrf.CsrfToken) request.getAttribute(org.springframework.security.web.csrf.CsrfToken.class.getName());
        if (csrfToken != null) {
            return ResponseEntity.ok(Map.of(
                "token", csrfToken.getToken(),
                "headerName", csrfToken.getHeaderName(),
                "parameterName", csrfToken.getParameterName()
            ));
        }
        return ResponseEntity.ok(Map.of("message", "CSRF token initialized"));
    }

    @Transactional(readOnly = true)
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Not logged in"));
        }

        String email = null;
        String username = authentication.getName();

        if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User oauth2User) {
            email = oauth2User.getAttribute("email");
        }

        Optional<User> userOpt = Optional.empty();
        if (email != null && !email.isBlank()) {
            userOpt = userRepository.findByEmail(email.toLowerCase().trim());
        }
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsername(username)
                    .or(() -> userRepository.findByEmail(username));
        }

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
        }

        User user = userOpt.get();
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("role", user.getRole());
        data.put("balance", user.getBalance() != null ? user.getBalance() : 0);
        data.put("phoneNumber", user.getPhoneNumber());
        data.put("email", user.getEmail());
        data.put("emailVerified", user.isEmailVerified());
        data.put("profilePictureUrl", user.getProfilePictureUrl());
        data.put("createdAt", user.getCreatedAt());
        data.put("authProvider", user.getAuthProvider());

        return ResponseEntity.ok(data);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        String usernameOrEmail = loginRequest.getUsername();
        if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Username atau email wajib diisi."));
        }

        // Cari user terlebih dahulu untuk memeriksa status verifikasi email
        Optional<User> userOpt = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail));

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Akun yang dibuat lewat Google tidak punya password lokal (SKPL-F03).
            if (user.getPassword() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message",
                        "Akun ini terdaftar melalui Google, silakan masuk dengan tombol Login dengan Google."));
            }

            // Cek kecocokan password
            if (passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                // KASUS: Akun belum diverifikasi
                if (!user.isEmailVerified()) {
                    try {
                        if (user.getEmail() != null && !user.getEmail().isBlank()) {
                            authService.resendOtp(user.getEmail());
                        }
                    } catch (Exception ignored) {
                        // cooldown still running: the previous code is still in the inbox
                    }

                    Map<String, Object> unverifiedResp = new HashMap<>();
                    unverifiedResp.put("success", false);
                    unverifiedResp.put("unverified", true);
                    unverifiedResp.put("email", user.getEmail() != null ? user.getEmail() : "");
                    unverifiedResp.put("message", "Akun Anda belum diverifikasi. Masukkan kode OTP yang dikirim ke email Anda.");
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(unverifiedResp);
                }
            }
        }

        try {
            // Login may use the email; Spring Security looks accounts up by username.
            String username = userOpt.map(User::getUsername).orElse(usernameOrEmail);
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, loginRequest.getPassword())
            );

            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(auth);
            SecurityContextHolder.setContext(securityContext);

            // New session id after login (session fixation protection).
            if (request.getSession(false) != null) {
                request.changeSessionId();
            }
            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

            User user = userOpt.orElseGet(() -> userRepository.findByUsername(loginRequest.getUsername()).orElseThrow());
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("username", user.getUsername());
            resp.put("role", user.getRole());
            resp.put("balance", user.getBalance());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.warn("Login failed for {}: {}", loginRequest.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Username atau password salah."));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRequest userRequest, BindingResult validation) {
        if (validation.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("success", false,
                    "message", validation.getAllErrors().get(0).getDefaultMessage()));
        }
        if (userRequest.getPassword() != null && !userRequest.getPassword().equals(userRequest.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Konfirmasi password tidak cocok."));
        }

        try {
            User saved = authService.registerUser(userRequest);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "needsVerification", true,
                    "email", saved.getEmail(),
                    "username", saved.getUsername(),
                    "message", "Kode OTP verifikasi telah dikirimkan ke email " + saved.getEmail()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp = body.get("otp");

        try {
            authService.verifyOtp(email, otp);
            return ResponseEntity.ok(Map.of("success", true, "message", "Email berhasil diverifikasi! Silakan masuk ke akun Anda."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");

        try {
            authService.resendOtp(email);
            return ResponseEntity.ok(Map.of("success", true, "message", "Kode OTP baru telah dikirimkan ke email Anda."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/forgot-password/request")
    public ResponseEntity<?> requestForgotPasswordOtp(@RequestBody Map<String, String> body) {
        String emailOrUsername = body.get("email");
        if (emailOrUsername == null || emailOrUsername.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Alamat email atau username wajib diisi."));
        }

        try {
            authService.requestPasswordReset(emailOrUsername);
            return ResponseEntity.ok(Map.of("success", true, "message", "Kode OTP reset password telah dikirim ke email Anda."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/forgot-password/confirm")
    public ResponseEntity<?> confirmForgotPasswordOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp = body.get("otp");
        String newPassword = body.get("newPassword");

        try {
            authService.confirmPasswordReset(email, otp, newPassword);
            return ResponseEntity.ok(Map.of("success", true, "message", "Password berhasil diubah! Silakan masuk dengan password baru Anda."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Logout berhasil."));
    }
}
