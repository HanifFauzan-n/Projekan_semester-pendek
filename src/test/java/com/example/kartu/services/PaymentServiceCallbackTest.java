package com.example.kartu.services;

import com.example.kartu.dto.request.XenditCallbackRequest;
import com.example.kartu.enums.TransactionStatus;
import com.example.kartu.models.TopUp;
import com.example.kartu.models.User;
import com.example.kartu.repositories.TopUpRepository;
import com.example.kartu.repositories.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Guards the Xendit webhook money path (SKPL-F05). No database and no Spring
 * context: these are the four rules that decide whether a balance gets credited.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceCallbackTest {

    private static final String EXTERNAL_ID = "topup-1-abcd1234";

    @Mock
    private TopUpRepository topUpRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private XenditService xenditService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PaymentService paymentService;

    private User user;
    private TopUp topUp;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(7);
        user.setUsername("Hanif18");
        user.setBalance(10000);

        topUp = new TopUp(user, 50000.0);
        topUp.setStatus(TransactionStatus.PENDING);
        topUp.setExternalId(EXTERNAL_ID);
    }

    private XenditCallbackRequest callback(String status, BigDecimal paidAmount) {
        XenditCallbackRequest callback = new XenditCallbackRequest();
        callback.setExternalId(EXTERNAL_ID);
        callback.setStatus(status);
        callback.setPaidAmount(paidAmount);
        callback.setPaymentMethod("EWALLET");
        callback.setPaymentChannel("OVO");
        return callback;
    }

    @Test
    void paidCallbackWithMatchingAmountCreditsBalance() {
        when(topUpRepository.findByExternalIdForUpdate(EXTERNAL_ID)).thenReturn(Optional.of(topUp));

        paymentService.handleCallback(callback("PAID", BigDecimal.valueOf(50000)));

        verify(userRepository).creditBalance(7, 50000);
        assertEquals(TransactionStatus.SUCCESS, topUp.getStatus());
        assertEquals("OVO", topUp.getPaymentChannel());
    }

    @Test
    void secondCallbackDoesNotCreditTwice() {
        topUp.setStatus(TransactionStatus.SUCCESS);
        when(topUpRepository.findByExternalIdForUpdate(EXTERNAL_ID)).thenReturn(Optional.of(topUp));

        paymentService.handleCallback(callback("PAID", BigDecimal.valueOf(50000)));

        verify(userRepository, never()).creditBalance(anyInt(), anyInt());
    }

    @Test
    void mismatchedAmountDoesNotCreditAndStaysPending() {
        when(topUpRepository.findByExternalIdForUpdate(EXTERNAL_ID)).thenReturn(Optional.of(topUp));

        paymentService.handleCallback(callback("PAID", BigDecimal.valueOf(10000)));

        assertEquals(TransactionStatus.PENDING, topUp.getStatus());
        verify(userRepository, never()).creditBalance(anyInt(), anyInt());
    }

    @Test
    void expiredCallbackFailsTopUpWithoutCrediting() {
        when(topUpRepository.findByExternalIdForUpdate(EXTERNAL_ID)).thenReturn(Optional.of(topUp));

        paymentService.handleCallback(callback("EXPIRED", null));

        assertEquals(TransactionStatus.FAILED, topUp.getStatus());
        verify(userRepository, never()).creditBalance(anyInt(), anyInt());
    }

    @Test
    void unknownExternalIdIsIgnored() {
        when(topUpRepository.findByExternalIdForUpdate(EXTERNAL_ID)).thenReturn(Optional.empty());

        paymentService.handleCallback(callback("PAID", BigDecimal.valueOf(50000)));

        verify(userRepository, never()).creditBalance(anyInt(), anyInt());
    }
}
