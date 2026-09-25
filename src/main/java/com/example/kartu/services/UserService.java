package com.example.kartu.services;

import com.example.kartu.dto.request.UserProfileRequest;
import com.example.kartu.models.User;
import com.example.kartu.models.UserAvatar;
import com.example.kartu.repositories.UserAvatarRepository;
import com.example.kartu.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;

import javax.imageio.ImageIO;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserAvatarRepository userAvatarRepository;
    private final PasswordEncoder passwordEncoder;

    private static final int AVATAR_SIZE = 256;
    private static final int MAX_UPLOAD_BYTES = 5 * 1024 * 1024;

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
        String newName = request.getUsername() == null ? "" : request.getUsername().trim().replaceAll("\\s+", " ");
        if (!newName.isEmpty() && !newName.equals(user.getUsername())) {
            userRepository.findByUsername(newName)
                    .filter(other -> !other.getId().equals(user.getId()))
                    .ifPresent(other -> {
                        throw new IllegalArgumentException("Nama pengguna sudah dipakai akun lain.");
                    });
            user.setUsername(newName);
        }

        String phone = request.getPhoneNumber().trim();
        if (userRepository.existsByPhoneNumberAndIdNot(phone, user.getId())) {
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

    /**
     * Stores a new profile photo. The upload (a data URL) is decoded, center-cropped and
     * re-encoded as a 256x256 JPEG, so whatever the browser sent is never served back as is.
     */
    @Transactional
    public void saveAvatar(User user, String dataUrl) {
        if (dataUrl == null || !dataUrl.startsWith("data:image/") || !dataUrl.contains(",")) {
            throw new IllegalArgumentException("Pilih file gambar (JPG, PNG, atau WebP).");
        }
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(dataUrl.substring(dataUrl.indexOf(',') + 1));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("File gambar rusak.");
        }
        if (raw.length > MAX_UPLOAD_BYTES) {
            throw new IllegalArgumentException("Ukuran gambar maksimal 5 MB.");
        }
        BufferedImage source;
        try {
            source = ImageIO.read(new ByteArrayInputStream(raw));
        } catch (IOException e) {
            source = null;
        }
        if (source == null) {
            throw new IllegalArgumentException("File bukan gambar yang didukung (JPG atau PNG).");
        }

        int side = Math.min(source.getWidth(), source.getHeight());
        BufferedImage square = new BufferedImage(AVATAR_SIZE, AVATAR_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = square.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setColor(Color.WHITE); // transparent PNGs get a white background (JPEG has no alpha)
        g.fillRect(0, 0, AVATAR_SIZE, AVATAR_SIZE);
        int x = (source.getWidth() - side) / 2;
        int y = (source.getHeight() - side) / 2;
        g.drawImage(source, 0, 0, AVATAR_SIZE, AVATAR_SIZE, x, y, x + side, y + side, null);
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(square, "jpg", out);
        } catch (IOException e) {
            throw new IllegalStateException("Gagal memproses gambar.", e);
        }

        UserAvatar avatar = userAvatarRepository.findById(user.getId()).orElseGet(UserAvatar::new);
        avatar.setUserId(user.getId());
        avatar.setImage(out.toByteArray());
        avatar.setUpdatedAt(OffsetDateTime.now());
        userAvatarRepository.save(avatar);

        // version query busts the browser cache after every change
        user.setProfilePictureUrl("/api/user/avatar/" + user.getId() + "?v=" + System.currentTimeMillis());
        userRepository.save(user);
    }

    @Transactional
    public void deleteAvatar(User user) {
        userAvatarRepository.deleteById(user.getId());
        user.setProfilePictureUrl(null);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Optional<byte[]> findAvatar(Integer userId) {
        return userAvatarRepository.findById(userId).map(UserAvatar::getImage);
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
