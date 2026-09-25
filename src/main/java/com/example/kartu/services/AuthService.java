package com.example.kartu.services;

import com.example.kartu.dto.request.UserRequest;
import com.example.kartu.models.User;
import com.example.kartu.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Registration, email verification and password reset with a 6-digit OTP.
 *
 * A 6-digit code is only safe with a guess limit: after MAX_OTP_ATTEMPTS wrong tries the
 * code is discarded and a new one must be requested, and new codes are rate limited by
 * OTP_RESEND_COOLDOWN. Methods that throw a checked Exception still commit (Spring only
 * rolls back on unchecked exceptions by default), which is what persists the attempt count.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    static final int MAX_OTP_ATTEMPTS = 5;
    static final Duration OTP_VALIDITY = Duration.ofMinutes(5);
    static final Duration OTP_RESEND_COOLDOWN = Duration.ofSeconds(60);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    private String generateOtp() {
        int code = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(code);
    }

    /** Puts a fresh OTP on the user, refusing when the previous one was sent too recently. */
    private String issueOtp(User user) throws Exception {
        LocalDateTime now = LocalDateTime.now();
        if (user.getOtpSentAt() != null && now.isBefore(user.getOtpSentAt().plus(OTP_RESEND_COOLDOWN))) {
            long wait = Duration.between(now, user.getOtpSentAt().plus(OTP_RESEND_COOLDOWN)).toSeconds() + 1;
            throw new Exception("Tunggu " + wait + " detik sebelum meminta kode OTP baru.");
        }
        String otp = generateOtp();
        user.setOtpCode(otp);
        user.setOtpExpiry(now.plus(OTP_VALIDITY));
        user.setOtpAttempts(0);
        user.setOtpSentAt(now);
        return otp;
    }

    /**
     * Checks a submitted OTP. A wrong guess is counted and saved before throwing; at the
     * limit the code is discarded so the remaining guesses cannot be used.
     */
    private void checkOtp(User user, String submitted) throws Exception {
        if (user.getOtpCode() == null) {
            throw new Exception("Kode OTP tidak berlaku. Silakan minta kode baru.");
        }
        if (user.getOtpExpiry() == null || LocalDateTime.now().isAfter(user.getOtpExpiry())) {
            throw new Exception("Kode OTP sudah kedaluwarsa. Silakan minta kode baru.");
        }
        boolean match = submitted != null && MessageDigest.isEqual(
                user.getOtpCode().getBytes(StandardCharsets.UTF_8),
                submitted.trim().getBytes(StandardCharsets.UTF_8));
        if (!match) {
            int attempts = (user.getOtpAttempts() == null ? 0 : user.getOtpAttempts()) + 1;
            user.setOtpAttempts(attempts);
            if (attempts >= MAX_OTP_ATTEMPTS) {
                user.setOtpCode(null);
                user.setOtpExpiry(null);
                userRepository.save(user);
                throw new Exception("Terlalu banyak percobaan salah. Kode OTP dibatalkan, silakan minta kode baru.");
            }
            userRepository.save(user);
            throw new Exception("Kode OTP salah. Sisa percobaan: " + (MAX_OTP_ATTEMPTS - attempts) + ".");
        }
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        user.setOtpAttempts(0);
    }

    /**
     * Mendaftarkan pengguna baru dengan OTP verifikasi email.
     * Jika akun sudah terdaftar tapi BELUM diverifikasi, sistem memperbarui data dan mengirim ulang OTP.
     */
    @Transactional(rollbackFor = Exception.class)
    public User registerUser(UserRequest requestUser) throws Exception {
        String email = requestUser.getEmail().trim().toLowerCase();
        String username = requestUser.getUsername().trim();

        Optional<User> existingByEmail = userRepository.findByEmail(email);
        Optional<User> existingByUsername = userRepository.findByUsername(username);

        User userToSave;

        if (existingByEmail.isPresent() && existingByEmail.get().isEmailVerified()) {
            throw new Exception("Alamat email sudah terdaftar dan terverifikasi. Silakan login.");
        }
        if (existingByUsername.isPresent() && existingByUsername.get().isEmailVerified()) {
            throw new Exception("Username sudah digunakan oleh akun lain.");
        }

        if (existingByEmail.isPresent()) {
            userToSave = existingByEmail.get();
            log.info("Akun {} ({}) sudah terdaftar namun belum terverifikasi. Mengirim ulang OTP baru.", username, email);
            userToSave.setUsername(username);
            userToSave.setPassword(passwordEncoder.encode(requestUser.getPassword()));
            userToSave.setPhoneNumber(requestUser.getPhoneNumber());
        } else if (existingByUsername.isPresent()) {
            userToSave = existingByUsername.get();
            log.info("Akun username {} sudah terdaftar namun belum terverifikasi. Mengirim ulang OTP baru.", username);
            userToSave.setEmail(email);
            userToSave.setPassword(passwordEncoder.encode(requestUser.getPassword()));
            userToSave.setPhoneNumber(requestUser.getPhoneNumber());
        } else {
            userToSave = new User();
            userToSave.setUsername(username);
            userToSave.setEmail(email);
            userToSave.setPassword(passwordEncoder.encode(requestUser.getPassword()));
            userToSave.setPhoneNumber(requestUser.getPhoneNumber());
            userToSave.setBalance(0);
            userToSave.setRole("ROLE_USER");
        }

        String otp = issueOtp(userToSave);
        userToSave.setEmailVerified(false);
        userToSave.setStatus("PENDING_VERIFICATION");

        User saved = userRepository.save(userToSave);
        emailService.sendVerificationOtp(email, username, otp);
        return saved;
    }

    @Transactional
    public boolean verifyOtp(String email, String otp) throws Exception {
        if (email == null || otp == null) {
            throw new Exception("Email dan kode OTP wajib diisi.");
        }

        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new Exception("Akun dengan email tersebut tidak ditemukan."));

        if (user.isEmailVerified()) {
            return true;
        }

        checkOtp(user, otp);

        user.setEmailVerified(true);
        user.setStatus("ACTIVE");
        userRepository.save(user);

        log.info("Email {} ({}) berhasil diverifikasi via OTP.", user.getEmail(), user.getUsername());
        EmailService.afterCommit(() -> emailService.sendWelcome(user.getEmail(), user.getUsername()));
        return true;
    }

    @Transactional
    public void resendOtp(String email) throws Exception {
        if (email == null || email.isBlank()) {
            throw new Exception("Email wajib diisi.");
        }

        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new Exception("Akun dengan email tersebut tidak ditemukan."));

        if (user.isEmailVerified()) {
            throw new Exception("Akun ini sudah terverifikasi. Silakan langsung login.");
        }

        String otp = issueOtp(user);
        userRepository.save(user);

        emailService.sendVerificationOtp(user.getEmail(), user.getUsername(), otp);
        log.info("OTP baru dikirim ulang ke {}", user.getEmail());
    }

    @Transactional
    public void requestPasswordReset(String emailOrUsername) throws Exception {
        if (emailOrUsername == null || emailOrUsername.isBlank()) {
            throw new Exception("Email atau username wajib diisi.");
        }

        String query = emailOrUsername.trim();
        User user = userRepository.findByEmail(query.toLowerCase())
                .or(() -> userRepository.findByUsername(query))
                .orElseThrow(() -> new Exception("Akun tidak ditemukan."));

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new Exception("Akun ini belum memiliki alamat email yang terdaftar.");
        }

        String otp = issueOtp(user);
        userRepository.save(user);

        emailService.sendPasswordResetOtp(user.getEmail(), user.getUsername(), otp);
        log.info("OTP reset password dikirim ke {}", user.getEmail());
    }

    @Transactional
    public void confirmPasswordReset(String emailOrUsername, String otp, String newPassword) throws Exception {
        if (emailOrUsername == null || otp == null || newPassword == null) {
            throw new Exception("Data tidak lengkap.");
        }
        if (newPassword.length() < 6) {
            throw new Exception("Password baru minimal 6 karakter.");
        }

        String query = emailOrUsername.trim();
        User user = userRepository.findByEmail(query.toLowerCase())
                .or(() -> userRepository.findByUsername(query))
                .orElseThrow(() -> new Exception("Akun tidak ditemukan."));

        checkOtp(user, otp);

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password untuk pengguna {} ({}) berhasil diperbarui via OTP.", user.getUsername(), user.getEmail());
    }
}
