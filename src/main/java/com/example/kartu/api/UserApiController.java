package com.example.kartu.api;

import com.example.kartu.dto.request.UserProfileRequest;
import com.example.kartu.models.User;
import com.example.kartu.services.CustomUserDetailsService;
import com.example.kartu.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

/** Kelola profil pelanggan (v1, dipindah ke React). /api/user/** butuh login (SecurityConfig). */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserApiController {

    private final UserService userService;
    private final CustomUserDetailsService userDetailsService;

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UserProfileRequest request, BindingResult validation,
                                           Authentication authentication, HttpServletRequest httpRequest) {
        User user = userService.findCurrentUser(authentication).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Silakan login terlebih dahulu."));
        }
        if (validation.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("success", false,
                    "message", validation.getAllErrors().get(0).getDefaultMessage()));
        }
        try {
            String oldUsername = user.getUsername();
            userService.updateProfile(user, request);
            // Form login identifies the session by username: move it to the new name, or the
            // next request would no longer find the account. Google sessions use the email.
            if (!oldUsername.equals(user.getUsername()) && !(authentication.getPrincipal() instanceof OAuth2User)) {
                UserDetails details = userDetailsService.loadUserByUsername(user.getUsername());
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                        details, null, details.getAuthorities()));
                SecurityContextHolder.setContext(context);
                httpRequest.getSession().setAttribute(
                        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
            }
            return ResponseEntity.ok(Map.of("success", true, "message", "Profil berhasil diperbarui."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestBody Map<String, String> body, Authentication authentication) {
        User user = userService.findCurrentUser(authentication).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Silakan login terlebih dahulu."));
        }
        try {
            userService.saveAvatar(user, body.get("image"));
            return ResponseEntity.ok(Map.of("success", true, "message", "Foto profil diperbarui.",
                    "profilePictureUrl", user.getProfilePictureUrl()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/avatar")
    public ResponseEntity<?> deleteAvatar(Authentication authentication) {
        User user = userService.findCurrentUser(authentication).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Silakan login terlebih dahulu."));
        }
        userService.deleteAvatar(user);
        return ResponseEntity.ok(Map.of("success", true, "message", "Foto profil dihapus."));
    }

    @GetMapping("/avatar/{userId}")
    public ResponseEntity<byte[]> avatar(@PathVariable Integer userId) {
        return userService.findAvatar(userId)
                .map(image -> ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePrivate())
                        .body(image))
                .orElse(ResponseEntity.notFound().build());
    }
}
