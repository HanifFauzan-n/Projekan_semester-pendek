package com.example.kartu.api;

import com.example.kartu.models.User;
import com.example.kartu.services.RatingService;
import com.example.kartu.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** SKPL-F21 (usulan): store rating endpoints for customers, the public summary, and admins. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RatingApiController {

    private final RatingService ratingService;
    private final UserService userService;

    @Transactional(readOnly = true)
    @GetMapping("/ratings/me")
    public ResponseEntity<?> myRating(Authentication authentication) {
        Optional<User> user = userService.findCurrentUser(authentication);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Silakan login terlebih dahulu."));
        }
        return ResponseEntity.ok(ratingService.eligibility(user.get()));
    }

    @PostMapping("/ratings")
    public ResponseEntity<?> submit(@RequestBody Map<String, Object> body, Authentication authentication) {
        Optional<User> user = userService.findCurrentUser(authentication);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Silakan login terlebih dahulu."));
        }
        Integer rating = body.get("rating") instanceof Number n ? n.intValue() : null;
        String comment = body.get("comment") instanceof String s ? s : null;
        try {
            ratingService.submit(user.get(), rating, comment);
            return ResponseEntity.ok(Map.of("success", true, "message", "Terima kasih atas penilaian Anda!"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            // two submits raced to create the same row; the unique constraint kept one
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("success", false,
                    "message", "Penilaian sedang diproses, silakan coba lagi."));
        }
    }

    @Transactional(readOnly = true)
    @GetMapping("/ratings/summary")
    public ResponseEntity<?> publicSummary() {
        Map<String, Object> summary = ratingService.summaryAllTime();
        return ResponseEntity.ok(Map.of("average", summary.get("average"), "count", summary.get("count")));
    }

    /** Public review page: summary + reviews with the admin reply, no login needed. */
    @Transactional(readOnly = true)
    @GetMapping("/ratings")
    public ResponseEntity<?> publicRatings(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(pageWithSummary(ratingService.publicList(page, size)));
    }

    @Transactional(readOnly = true)
    @GetMapping("/admin/ratings")
    public ResponseEntity<?> adminRatings(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size,
                                          @RequestParam(defaultValue = "false") boolean unreplied) {
        Map<String, Object> result = pageWithSummary(ratingService.adminList(page, size, unreplied));
        result.put("unrepliedCount", ratingService.countUnreplied());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/admin/ratings/{id}/reply")
    public ResponseEntity<?> reply(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String reply = body.get("reply") instanceof String s ? s : null;
        try {
            ratingService.reply(id, reply);
            return ResponseEntity.ok(Map.of("success", true,
                    "message", reply == null || reply.isBlank() ? "Balasan dihapus." : "Balasan tersimpan."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    private Map<String, Object> pageWithSummary(Page<Map<String, Object>> items) {
        Map<String, Object> result = new LinkedHashMap<>(ratingService.summaryAllTime());
        result.put("items", items.getContent());
        result.put("page", items.getNumber());
        result.put("totalPages", items.getTotalPages());
        result.put("totalElements", items.getTotalElements());
        return result;
    }
}
