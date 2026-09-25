package com.example.kartu.api;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.kartu.enums.PurchaseTarget;
import com.example.kartu.models.ProductOrder;
import com.example.kartu.models.User;
import com.example.kartu.repositories.TransactionHistoryRepository;
import com.example.kartu.services.OrderService;
import com.example.kartu.services.UserService;
import com.example.kartu.services.XenditService;

import lombok.RequiredArgsConstructor;

/** Paying a product directly through Xendit (+10% admin fee) instead of the balance. */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderService orderService;
    private final UserService userService;
    private final TransactionHistoryRepository transactionHistoryRepository;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body, Authentication authentication) {
        User user = userService.findCurrentUser(authentication).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Silakan login terlebih dahulu."));
        }
        if (!(body.get("productId") instanceof Integer productId)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Produk wajib dipilih."));
        }
        try {
            ProductOrder order = orderService.createXenditOrder(user, productId,
                    body.get("voucherCode") instanceof String v ? v : null,
                    body.get("customerNumber") instanceof String n ? n : null);
            return ResponseEntity.ok(Map.of(
                    "externalId", order.getExternalId(),
                    "invoiceUrl", order.getInvoiceUrl(),
                    "price", order.getPrice(),
                    "adminFee", order.getAdminFee(),
                    "total", order.getTotal()));
        } catch (XenditService.XenditException e) {
            return ResponseEntity.status(502).body(Map.of("message",
                    "Pembayaran Xendit sedang tidak tersedia. Coba lagi nanti atau bayar dengan saldo."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    e.getMessage() != null ? e.getMessage() : "Pesanan gagal dibuat."));
        }
    }

    /** Status for /pesanan/:externalId (the Xendit return page). Only the buyer can read it. */
    @Transactional(readOnly = true)
    @GetMapping("/{externalId}")
    public ResponseEntity<?> status(@PathVariable String externalId, Authentication authentication) {
        User user = userService.findCurrentUser(authentication).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Silakan login terlebih dahulu."));
        }
        return orderService.findForUser(externalId, user)
                .<ResponseEntity<?>>map(o -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("externalId", o.getExternalId());
                    m.put("status", o.getStatus());
                    m.put("note", o.getNote());
                    m.put("productName", o.getProduct().getName());
                    m.put("target", PurchaseTarget.of(o.getProduct()));
                    m.put("customerNumber", o.getCustomerNumber());
                    m.put("price", o.getPrice());
                    m.put("adminFee", o.getAdminFee());
                    m.put("total", o.getTotal());
                    m.put("invoiceUrl", o.getInvoiceUrl());
                    m.put("paymentChannel", o.getPaymentChannel());
                    m.put("transactionId", o.getTransactionId());
                    if (o.getTransactionId() != null) {
                        transactionHistoryRepository.findByTransactionId(o.getTransactionId())
                                .ifPresent(tx -> m.put("serialNumber", tx.getSerialNumber()));
                    }
                    return ResponseEntity.ok(m);
                })
                .orElse(ResponseEntity.status(404).body(Map.of("message", "Pesanan tidak ditemukan.")));
    }
}
