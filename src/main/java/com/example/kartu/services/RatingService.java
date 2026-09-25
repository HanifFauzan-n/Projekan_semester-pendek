package com.example.kartu.services;

import com.example.kartu.enums.TransactionStatus;
import com.example.kartu.models.StoreRating;
import com.example.kartu.models.User;
import com.example.kartu.repositories.StoreRatingRepository;
import com.example.kartu.repositories.TransactionHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * SKPL-F21 (usulan): "Nilai Kami" store rating.
 *
 * - Every logged-in customer may rate (one rating per customer, editable).
 * - The automatic popup is only offered after MIN_TRANSACTIONS successful purchases.
 * - Reviews are public; the admin can reply. When the customer edits the review the old
 *   reply is cleared, so the admin sees it as unanswered again.
 */
@Service
@RequiredArgsConstructor
public class RatingService {

    public static final int MIN_TRANSACTIONS = 2;
    public static final int MAX_COMMENT_LENGTH = 500;
    public static final int MAX_REPLY_LENGTH = 1000;

    // Wide enough for "all time" without hitting timestamptz limits.
    private static final OffsetDateTime ALL_TIME_FROM = OffsetDateTime.parse("2000-01-01T00:00:00Z");

    private final StoreRatingRepository ratingRepository;
    private final TransactionHistoryRepository transactionRepository;

    public Map<String, Object> eligibility(User user) {
        long count = transactionRepository.countByUserIdAndStatus(user.getId(), TransactionStatus.SUCCESS);
        Optional<StoreRating> existing = ratingRepository.findByUserId(user.getId());

        Map<String, Object> result = new HashMap<>();
        // "eligible" drives the automatic popup only; rating itself is open to every customer.
        result.put("eligible", count >= MIN_TRANSACTIONS && existing.isEmpty());
        result.put("successfulTransactions", count);
        result.put("minTransactions", MIN_TRANSACTIONS);
        result.put("rating", existing.map(RatingService::toMap).orElse(null));
        return result;
    }

    /** Creates or updates the caller's rating. Throws IllegalArgumentException with a user-facing message. */
    @Transactional
    public StoreRating submit(User user, Integer rating, String comment) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Pilih rating 1 sampai 5 bintang.");
        }
        String text = clean(comment);
        if (text != null && text.length() > MAX_COMMENT_LENGTH) {
            throw new IllegalArgumentException("Komentar maksimal " + MAX_COMMENT_LENGTH + " karakter.");
        }

        StoreRating stored = ratingRepository.findByUserId(user.getId()).orElseGet(() -> {
            StoreRating fresh = new StoreRating();
            fresh.setUser(user);
            return fresh;
        });
        boolean changed = stored.getId() == null
                || stored.getRating() != rating.shortValue()
                || !Objects.equals(stored.getComment(), text);
        if (changed && stored.getId() != null) {
            // the old reply answered a different review
            stored.setAdminReply(null);
            stored.setRepliedAt(null);
        }
        stored.setRating(rating.shortValue());
        stored.setComment(text);
        if (changed) {
            stored.setUpdatedAt(OffsetDateTime.now());
        }
        return ratingRepository.saveAndFlush(stored);
    }

    /** Saves or, with a blank text, removes the admin reply. Throws IllegalArgumentException. */
    @Transactional
    public StoreRating reply(Long ratingId, String reply) {
        StoreRating stored = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new IllegalArgumentException("Ulasan tidak ditemukan."));
        String text = clean(reply);
        if (text != null && text.length() > MAX_REPLY_LENGTH) {
            throw new IllegalArgumentException("Balasan maksimal " + MAX_REPLY_LENGTH + " karakter.");
        }
        stored.setAdminReply(text);
        stored.setRepliedAt(text == null ? null : OffsetDateTime.now());
        return ratingRepository.saveAndFlush(stored);
    }

    /** Average (1 decimal), count and 1..5 distribution of ratings given or updated in [from, to). */
    public Map<String, Object> summary(OffsetDateTime from, OffsetDateTime to) {
        Object[] row = ratingRepository.summarize(from, to).get(0);
        double average = ((Number) row[0]).doubleValue();
        long count = ((Number) row[1]).longValue();

        Map<String, Long> distribution = new LinkedHashMap<>();
        for (int star = 5; star >= 1; star--) {
            distribution.put(String.valueOf(star), 0L);
        }
        for (Object[] r : ratingRepository.distribution(from, to)) {
            distribution.put(String.valueOf(((Number) r[0]).intValue()), ((Number) r[1]).longValue());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("average", Math.round(average * 10) / 10.0);
        result.put("count", count);
        result.put("distribution", distribution);
        return result;
    }

    public Map<String, Object> summaryAllTime() {
        return summary(ALL_TIME_FROM, OffsetDateTime.now().plusDays(1));
    }

    /** Latest comments in the period, without who wrote them (sent to the AI chat). */
    public List<Map<String, Object>> latestComments(OffsetDateTime from, OffsetDateTime to) {
        return ratingRepository
                .findTop20ByCommentIsNotNullAndUpdatedAtGreaterThanEqualAndUpdatedAtLessThanOrderByUpdatedAtDesc(from, to)
                .stream()
                .map(r -> Map.<String, Object>of(
                        "rating", r.getRating(),
                        "comment", r.getComment(),
                        "date", r.getUpdatedAt().toLocalDate().toString()))
                .toList();
    }

    /** Public list, newest first. */
    public Page<Map<String, Object>> publicList(int page, int size) {
        return ratingRepository.findAllByOrderByUpdatedAtDesc(pageable(page, size)).map(RatingService::toListItem);
    }

    /** Admin list; {@code unrepliedOnly} shows the reviews that still need an answer. */
    public Page<Map<String, Object>> adminList(int page, int size, boolean unrepliedOnly) {
        Pageable p = pageable(page, size);
        Page<StoreRating> result = unrepliedOnly
                ? ratingRepository.findByAdminReplyIsNullOrderByUpdatedAtDesc(p)
                : ratingRepository.findAllByOrderByUpdatedAtDesc(p);
        return result.map(RatingService::toListItem);
    }

    public long countUnreplied() {
        return ratingRepository.countByAdminReplyIsNull();
    }

    private static Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
    }

    private static String clean(String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Map<String, Object> toListItem(StoreRating r) {
        Map<String, Object> m = toMap(r);
        m.put("id", r.getId());
        m.put("username", r.getUser().getUsername());
        return m;
    }

    private static Map<String, Object> toMap(StoreRating r) {
        Map<String, Object> m = new HashMap<>();
        m.put("rating", r.getRating());
        m.put("comment", r.getComment());
        m.put("createdAt", r.getCreatedAt());
        m.put("updatedAt", r.getUpdatedAt());
        m.put("adminReply", r.getAdminReply());
        m.put("repliedAt", r.getRepliedAt());
        return m;
    }
}
