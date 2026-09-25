package com.example.kartu.services;

import com.example.kartu.enums.TransactionStatus;
import com.example.kartu.models.StoreRating;
import com.example.kartu.models.User;
import com.example.kartu.repositories.StoreRatingRepository;
import com.example.kartu.repositories.TransactionHistoryRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Rules of the "Nilai Kami" rating: 1-5 stars, comment <= 500 chars, one row per user, admin reply. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RatingServiceTest {

    @Mock
    private StoreRatingRepository ratingRepository;

    @Mock
    private TransactionHistoryRepository transactionRepository;

    @InjectMocks
    private RatingService ratingService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(5);
        when(transactionRepository.countByUserIdAndStatus(5, TransactionStatus.SUCCESS)).thenReturn(2L);
        when(ratingRepository.saveAndFlush(any(StoreRating.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void starsOutsideOneToFiveAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> ratingService.submit(user, 0, null));
        assertThrows(IllegalArgumentException.class, () -> ratingService.submit(user, 6, null));
        assertThrows(IllegalArgumentException.class, () -> ratingService.submit(user, null, null));
    }

    @Test
    void commentLongerThan500IsRejectedAndBlankBecomesNull() {
        assertThrows(IllegalArgumentException.class, () -> ratingService.submit(user, 4, "a".repeat(501)));
        when(ratingRepository.findByUserId(5)).thenReturn(Optional.empty());
        assertNull(ratingService.submit(user, 4, "   ").getComment());
        assertEquals(500, ratingService.submit(user, 4, "a".repeat(500)).getComment().length());
    }

    @Test
    void anyLoggedInCustomerMayRateWithoutPurchases() {
        when(transactionRepository.countByUserIdAndStatus(5, TransactionStatus.SUCCESS)).thenReturn(0L);
        when(ratingRepository.findByUserId(5)).thenReturn(Optional.empty());
        assertEquals((short) 5, ratingService.submit(user, 5, "mantap").getRating());
    }

    @Test
    void popupIsOfferedOnlyFromTwoPurchasesAndOnlyOnce() {
        when(ratingRepository.findByUserId(5)).thenReturn(Optional.empty());
        when(transactionRepository.countByUserIdAndStatus(5, TransactionStatus.SUCCESS)).thenReturn(1L);
        assertEquals(false, ratingService.eligibility(user).get("eligible"));
        when(transactionRepository.countByUserIdAndStatus(5, TransactionStatus.SUCCESS)).thenReturn(2L);
        assertEquals(true, ratingService.eligibility(user).get("eligible"));
        when(ratingRepository.findByUserId(5)).thenReturn(Optional.of(new StoreRating()));
        assertEquals(false, ratingService.eligibility(user).get("eligible"));
    }

    @Test
    void editingTheReviewClearsTheOldReplyButResubmittingTheSameDoesNot() {
        StoreRating existing = new StoreRating();
        existing.setId(9L);
        existing.setUser(user);
        existing.setRating((short) 3);
        existing.setComment("lumayan");
        existing.setAdminReply("Terima kasih!");
        when(ratingRepository.findByUserId(5)).thenReturn(Optional.of(existing));

        ratingService.submit(user, 3, " lumayan ");
        assertEquals("Terima kasih!", existing.getAdminReply());

        ratingService.submit(user, 4, "lumayan");
        assertNull(existing.getAdminReply());
    }

    @Test
    void replyIsLimitedAndBlankRemovesIt() {
        StoreRating existing = new StoreRating();
        existing.setId(9L);
        when(ratingRepository.findById(9L)).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class, () -> ratingService.reply(9L, "a".repeat(1001)));
        ratingService.reply(9L, "Terima kasih, stok Tri sudah kami tambah.");
        assertNotNull(existing.getRepliedAt());
        ratingService.reply(9L, "   ");
        assertNull(existing.getAdminReply());
        assertNull(existing.getRepliedAt());
    }

    @Test
    void secondSubmitUpdatesTheSameRow() {
        StoreRating existing = new StoreRating();
        existing.setId(9L);
        existing.setUser(user);
        existing.setRating((short) 2);
        when(ratingRepository.findByUserId(5)).thenReturn(Optional.of(existing));

        StoreRating saved = ratingService.submit(user, 5, "sudah membaik");

        assertSame(existing, saved);
        assertEquals((short) 5, saved.getRating());
        assertEquals("sudah membaik", saved.getComment());
    }
}
