package com.example.kartu.api;

import com.example.kartu.dto.request.XenditCallbackRequest;
import com.example.kartu.services.OrderService;
import com.example.kartu.services.PaymentService;
import com.example.kartu.services.XenditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Xendit webhook endpoint (SKPL-F05).
 * Mendukung kedua format path: /api/xendit/callback dan /api/xendit/webhook
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class XenditWebhookController {

    private final XenditService xenditService;
    private final PaymentService paymentService;
    private final OrderService orderService;

    @PostMapping({"/api/xendit/callback", "/api/xendit/webhook"})
    public ResponseEntity<String> callback(
            @RequestHeader(value = "x-callback-token", required = false) String token,
            @RequestBody(required = false) XenditCallbackRequest callback) {

        log.info("--> Menerima Webhook Xendit, token={}, body={}", token != null ? "Ada" : "Kosong", callback != null ? callback.getExternalId() : "null");

        if (!xenditService.isValidCallbackToken(token)) {
            log.warn("Header x-callback-token Xendit tidak cocok atau kosong! Webhook ditolak (401).");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid callback token");
        }

        // order-... = product paid directly (OrderService); everything else is a balance top up.
        if (callback != null && callback.getExternalId() != null
                && callback.getExternalId().startsWith(OrderService.EXTERNAL_ID_PREFIX)) {
            orderService.handleCallback(callback);
        } else {
            paymentService.handleCallback(callback);
        }
        log.info("<-- Webhook Xendit berhasil diproses.");
        return ResponseEntity.ok("ok");
    }
}
