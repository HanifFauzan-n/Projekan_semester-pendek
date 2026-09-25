package com.example.kartu.services;

import com.example.kartu.enums.TransactionStatus;
import com.example.kartu.models.TopUp;
import com.example.kartu.models.User;
import com.example.kartu.repositories.TopUpRepository;
import com.example.kartu.repositories.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Manual top up money path: a request is only credited through one successful admin approval. */
@ExtendWith(MockitoExtension.class)
class TopUpServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TopUpRepository topUpRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private TopUpService topUpService;

    private TopUp pendingManual() {
        User user = new User();
        user.setId(3);
        TopUp topUp = new TopUp(user, 50000.0);
        topUp.setId(11);
        topUp.setStatus(TransactionStatus.PENDING);
        return topUp;
    }

    @Test
    void approveCreditsOnce() {
        when(topUpRepository.findById(11)).thenReturn(Optional.of(pendingManual()));
        when(topUpRepository.resolvePendingManual(11, TransactionStatus.SUCCESS)).thenReturn(1);

        topUpService.approveManual(11);

        verify(userRepository).creditBalance(3, 50000);
    }

    @Test
    void approvingAnAlreadyProcessedOrXenditTopUpDoesNotCredit() {
        when(topUpRepository.findById(11)).thenReturn(Optional.of(pendingManual()));
        when(topUpRepository.resolvePendingManual(11, TransactionStatus.SUCCESS)).thenReturn(0);

        assertThrows(IllegalStateException.class, () -> topUpService.approveManual(11));
        verify(userRepository, never()).creditBalance(anyInt(), anyInt());
    }
}
