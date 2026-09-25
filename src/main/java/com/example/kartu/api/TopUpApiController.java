package com.example.kartu.api;

import com.example.kartu.models.TopUp;
import com.example.kartu.models.User;
import com.example.kartu.repositories.TopUpRepository;
import com.example.kartu.repositories.UserRepository;
import com.example.kartu.services.PaymentService;
import com.example.kartu.services.TopUpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/topups")
@RequiredArgsConstructor
@Slf4j
public class TopUpApiController {

    private final PaymentService paymentService;
    private final TopUpService topUpService;
    private final UserRepository userRepository;
    private final TopUpRepository topUpRepository;

    @PostMapping("/pay")
    public ResponseEntity<?> payXendit(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "Silakan login terlebih dahulu."));
        }

        Number amountNum = (Number) body.get("amount");
        if (amountNum == null || amountNum.doubleValue() < 10000) {
            return ResponseEntity.badRequest().body(Map.of("message", "Minimal top up adalah Rp 10.000"));
        }

        String usernameOrEmail = authentication.getName();
        if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User oauth2User) {
            String email = oauth2User.getAttribute("email");
            if (email != null && !email.isBlank()) usernameOrEmail = email;
        }

        try {
            String invoiceUrl = paymentService.createInvoice(usernameOrEmail, amountNum.doubleValue());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "invoiceUrl", invoiceUrl,
                    "amount", amountNum.doubleValue(),
                    "message", "Invoice Xendit berhasil dibuat."
            ));
        } catch (Exception e) {
            log.error("Gagal membuat invoice Xendit", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Gagal memproses pembayaran otomatis: " + e.getMessage()
            ));
        }
    }

    @Transactional(readOnly = true)
    @GetMapping("/status/{externalId}")
    public ResponseEntity<?> getStatus(@PathVariable String externalId) {
        Optional<TopUp> topUpOpt = paymentService.findByExternalId(externalId);
        if (topUpOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        TopUp t = topUpOpt.get();

        Map<String, Object> map = new HashMap<>();
        map.put("id", t.getId());
        map.put("externalId", t.getExternalId());
        map.put("amount", t.getAmount());
        map.put("status", t.getStatus());
        map.put("paymentMethod", t.getPaymentMethod());
        map.put("paymentChannel", t.getPaymentChannel());
        map.put("paidAt", t.getPaidAt());
        map.put("date", t.getDate());

        return ResponseEntity.ok(map);
    }

    @Transactional(readOnly = true)
    @GetMapping("/my")
    public ResponseEntity<?> getMyTopUps(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "Silakan login terlebih dahulu."));
        }

        String usernameOrEmail = authentication.getName();
        if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User oauth2User) {
            String email = oauth2User.getAttribute("email");
            if (email != null && !email.isBlank()) {
                usernameOrEmail = email;
            }
        }

        String finalId = usernameOrEmail;
        Optional<User> userOpt = userRepository.findByUsername(finalId)
                .or(() -> userRepository.findByEmail(finalId));
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<TopUp> list = topUpRepository.findByUserIdOrderByDateDesc(userOpt.get().getId());
        List<Map<String, Object>> result = list.stream().map(t -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", t.getId());
            map.put("amount", t.getAmount());
            map.put("status", t.getStatus());
            map.put("externalId", t.getExternalId());
            map.put("paymentChannel", t.getPaymentChannel());
            map.put("date", t.getDate());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
