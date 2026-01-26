package com.example.kartu.services;

import com.example.kartu.models.User;
import com.example.kartu.repositories.TopUpRepository;
import com.example.kartu.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class TopUpServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TopUpRepository topUpRepository;

    @InjectMocks
    private TopUpService topUpService;

    @Test
    void testTopUpSuccess() throws Exception {
        // 1. Skenario: User Hanif mau Top Up 50.000
        String username = "Hanif18";
        Double amount = 50000.0;

        User mockUser = new User();
        mockUser.setUsername(username);
        mockUser.setBalance(10000); // Saldo awal 10rb

        // Pura-pura database menemukan user
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(mockUser));

        // 2. Eksekusi Service
        topUpService.processTopUp(username, amount);

        // 3. Verifikasi Hasil (Assert)
        // Saldo harusnya jadi 10rb + 50rb = 60rb
        assertEquals(60000, mockUser.getBalance());
        
        // Pastikan fungsi save dipanggil 1 kali
        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    void testTopUpFailed_MinimumAmount() {
        // Skenario: Top Up cuma 5.000 (Padahal minimal 10.000)
        Exception exception = assertThrows(Exception.class, () -> {
            topUpService.processTopUp("Hanif18", 5000.0);
        });

        assertEquals("Minimal Top Up Rp 10.000", exception.getMessage());
    }
}