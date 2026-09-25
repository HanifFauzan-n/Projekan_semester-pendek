package com.example.kartu.api;

import com.example.kartu.enums.PurchaseTarget;
import com.example.kartu.models.TransactionHistory;
import com.example.kartu.models.User;
import com.example.kartu.repositories.TransactionHistoryRepository;
import com.example.kartu.repositories.UserRepository;
import com.example.kartu.services.TransactionHistoryService;
import com.example.kartu.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionApiController {

    private final TransactionHistoryService transactionHistoryService;
    private final UserRepository userRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final UserService userService;

    /**
     * Data for the printable receipt (/struk/:transactionId). Only the buyer or an admin may read it;
     * anyone else gets 404 so transaction IDs cannot be probed.
     */
    @Transactional(readOnly = true)
    @GetMapping("/{transactionId}/receipt")
    public ResponseEntity<?> getReceipt(@PathVariable String transactionId, Authentication authentication) {
        boolean admin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        Integer me = userService.findCurrentUser(authentication).map(User::getId).orElse(null);
        return transactionHistoryRepository.findByTransactionId(transactionId)
                .filter(tx -> admin || (tx.getUser() != null && tx.getUser().getId().equals(me)))
                .<ResponseEntity<?>>map(tx -> {
                    Map<String, Object> r = new HashMap<>();
                    double paid = tx.getAmountPaid() == null ? 0 : tx.getAmountPaid();
                    double discount = tx.getDiscountAmount() == null ? 0 : tx.getDiscountAmount();
                    r.put("transactionId", tx.getTransactionId());
                    r.put("timestamp", tx.getTimestamp());
                    r.put("customer", tx.getCustomer());
                    r.put("productName", tx.getProduct() != null ? tx.getProduct().getName() : "-");
                    r.put("target", PurchaseTarget.of(tx.getProduct()));
                    r.put("customerNumber", tx.getCustomerNumber());
                    r.put("serialNumber", tx.getSerialNumber());
                    r.put("price", paid + discount);
                    r.put("discountAmount", discount);
                    r.put("amountPaid", paid);
                    r.put("paymentMethod", tx.getPaymentMethod());
                    r.put("adminFee", tx.getAdminFee() == null ? 0 : tx.getAdminFee());
                    r.put("status", tx.getStatus());
                    return ResponseEntity.ok(r);
                })
                .orElse(ResponseEntity.status(404).body(Map.of("message", "Struk tidak ditemukan.")));
    }

    @PostMapping("/buy")
    public ResponseEntity<?> buyProduct(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "Silakan login terlebih dahulu."));
        }

        Integer productId = (Integer) body.get("productId");
        String voucherCode = (String) body.get("voucherCode");
        String customerNumber = body.get("customerNumber") instanceof String n ? n : null;

        if (productId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Produk wajib dipilih."));
        }

        String usernameOrEmail = authentication.getName();
        if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User oauth2User) {
            String email = oauth2User.getAttribute("email");
            if (email != null && !email.isBlank()) {
                usernameOrEmail = email;
            }
        }

        String finalUsernameOrEmail = usernameOrEmail;

        try {
            User user = userRepository.findByUsername(finalUsernameOrEmail)
                    .or(() -> userRepository.findByEmail(finalUsernameOrEmail))
                    .orElseThrow(() -> new Exception("User tidak ditemukan."));

            TransactionHistory tx = transactionHistoryService.purchaseProduct(productId, user.getUsername(), voucherCode, customerNumber);

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("transactionId", tx.getTransactionId());
            resp.put("serialNumber", tx.getSerialNumber());
            resp.put("productName", tx.getProduct().getName());
            resp.put("customerNumber", tx.getCustomerNumber());
            resp.put("target", PurchaseTarget.of(tx.getProduct()));
            resp.put("amountPaid", tx.getAmountPaid());
            resp.put("discountAmount", tx.getDiscountAmount());
            resp.put("newBalance", userRepository.findBalanceById(user.getId()));
            resp.put("message", "Pembelian berhasil!");

            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            Map<String, Object> errResp = new HashMap<>();
            errResp.put("success", false);
            errResp.put("message", e.getMessage() != null ? e.getMessage() : "Transaksi gagal diproses.");
            return ResponseEntity.badRequest().body(errResp);
        }
    }

    @Transactional(readOnly = true)
    @GetMapping("/my")
    public ResponseEntity<?> getMyTransactions(Authentication authentication) {
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

        String finalUsernameOrEmail = usernameOrEmail;

        Optional<User> userOpt = userRepository.findByUsername(finalUsernameOrEmail)
                .or(() -> userRepository.findByEmail(finalUsernameOrEmail));
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<TransactionHistory> list = transactionHistoryService.getTransactionHistoryByUser(userOpt.get());
        List<Map<String, Object>> result = list.stream().map(tx -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", tx.getId());
            map.put("transactionId", tx.getTransactionId());
            map.put("serialNumber", tx.getSerialNumber());
            map.put("productName", tx.getProduct() != null ? tx.getProduct().getName() : "-");
            map.put("customerNumber", tx.getCustomerNumber());
            map.put("target", PurchaseTarget.of(tx.getProduct()));
            map.put("paymentMethod", tx.getPaymentMethod());
            map.put("adminFee", tx.getAdminFee() == null ? 0 : tx.getAdminFee());
            map.put("amountPaid", tx.getAmountPaid());
            map.put("discountAmount", tx.getDiscountAmount());
            map.put("status", tx.getStatus());
            map.put("timestamp", tx.getTimestamp());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
