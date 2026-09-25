package com.example.kartu.services;

import com.example.kartu.models.User;
import com.example.kartu.repositories.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** A 6-digit OTP is only safe with a guess limit and a resend cooldown. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceOtpTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("salsa08");
        user.setEmail("salsa@contoh.id");
        user.setPassword("hash");
        user.setOtpCode("123456");
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        user.setOtpAttempts(0);
        user.setOtpSentAt(LocalDateTime.now());
        when(userRepository.findByEmail("salsa@contoh.id")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("new-hash");
    }

    @Test
    void fifthWrongGuessDiscardsTheCode() {
        for (int i = 1; i <= 4; i++) {
            Exception e = assertThrows(Exception.class,
                    () -> authService.confirmPasswordReset("salsa@contoh.id", "000000", "rahasia99"));
            assertTrue(e.getMessage().contains("Sisa percobaan: " + (5 - i)), e.getMessage());
        }
        Exception last = assertThrows(Exception.class,
                () -> authService.confirmPasswordReset("salsa@contoh.id", "000000", "rahasia99"));
        assertTrue(last.getMessage().startsWith("Terlalu banyak percobaan"));
        assertNull(user.getOtpCode());

        // even the right code is useless now
        assertThrows(Exception.class, () -> authService.confirmPasswordReset("salsa@contoh.id", "123456", "rahasia99"));
        assertEquals("hash", user.getPassword());
    }

    @Test
    void rightCodeResetsPasswordAndIsSingleUse() throws Exception {
        authService.confirmPasswordReset("salsa@contoh.id", " 123456 ", "rahasia99");
        assertEquals("new-hash", user.getPassword());
        assertNull(user.getOtpCode());
        assertThrows(Exception.class, () -> authService.confirmPasswordReset("salsa@contoh.id", "123456", "lagi123"));
    }

    @Test
    void newCodeWithinCooldownIsRefused() throws Exception {
        assertThrows(Exception.class, () -> authService.requestPasswordReset("salsa@contoh.id"));
        verify(emailService, never()).sendPasswordResetOtp(anyString(), anyString(), anyString());

        user.setOtpSentAt(LocalDateTime.now().minusSeconds(61));
        authService.requestPasswordReset("salsa@contoh.id");
        verify(emailService).sendPasswordResetOtp(eq("salsa@contoh.id"), eq("salsa08"), anyString());
        assertEquals(0, user.getOtpAttempts());
    }
}
