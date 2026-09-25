package com.example.kartu.services;

import com.example.kartu.models.User;
import com.example.kartu.repositories.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** SKPL-F03 account mapping: no duplicates by email, and Google login cannot lift a ban. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GoogleAccountServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private GoogleAccountService service;

    private User existing(String status) {
        User u = new User();
        u.setUsername("rizky21");
        u.setEmail("rizky@gmail.com");
        u.setStatus(status);
        when(userRepository.findByEmail("rizky@gmail.com")).thenReturn(Optional.of(u));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        return u;
    }

    @Test
    void bannedAccountStaysBannedAndLoginIsRefused() {
        User banned = existing("BANNED");
        assertThrows(OAuth2AuthenticationException.class,
                () -> service.upsert("Rizky@Gmail.com", "Rizky", "sub-1", null));
        assertEquals("BANNED", banned.getStatus());
        verify(userRepository, never()).save(any());
    }

    @Test
    void existingEmailIsLinkedNotDuplicated() {
        User u = existing("PENDING_VERIFICATION");
        User result = service.upsert("rizky@gmail.com", "Rizky", "sub-1", "https://pic");
        assertSame(u, result);
        assertEquals("ACTIVE", u.getStatus());
        assertTrue(u.isEmailVerified());
        assertEquals("sub-1", u.getProviderId());
        verify(emailService, never()).sendWelcome(any(), any());
    }

    @Test
    void newEmailCreatesUserWithUniqueUsername() {
        when(userRepository.findByEmail("baru@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("sitiaminah")).thenReturn(Optional.of(new User()));
        when(userRepository.findByUsername("sitiaminah1")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User created = service.upsert("baru@gmail.com", "Siti Aminah", "sub-2", null);

        assertEquals("sitiaminah1", created.getUsername());
        assertEquals("ROLE_USER", created.getRole());
        assertEquals(0, created.getBalance());
        assertNull(created.getPassword());
        verify(emailService).sendWelcome("baru@gmail.com", "sitiaminah1");
    }
}
