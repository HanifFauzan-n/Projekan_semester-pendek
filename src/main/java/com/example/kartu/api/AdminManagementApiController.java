package com.example.kartu.api;

import com.example.kartu.enums.TransactionStatus;
import com.example.kartu.models.*;
import com.example.kartu.repositories.*;
import com.example.kartu.services.TopUpService;
import com.example.kartu.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminManagementApiController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProviderRepository providerRepository;
    private final VoucherRepository voucherRepository;
    private final FlashSaleRepository flashSaleRepository;
    private final TopUpRepository topUpRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final TopUpService topUpService;

    // --- Product Management ---
    @Transactional(readOnly = true)
    @GetMapping("/products")
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productRepository.findAll());
    }

    @PostMapping("/products")
    public ResponseEntity<?> saveProduct(@RequestBody Map<String, Object> body) {
        Integer id = (Integer) body.get("id");
        String name = (String) body.get("name");
        Integer price = (Integer) body.get("price");
        Integer costPrice = (Integer) body.get("costPrice");
        Integer stock = (Integer) body.get("stock");
        String description = (String) body.get("description");
        String categoryId = (String) body.get("categoryId");
        Integer providerId = (Integer) body.get("providerId");

        Product product = (id != null) ? productRepository.findById(id).orElse(new Product()) : new Product();
        product.setName(name);
        product.setPrice(price);
        product.setCostPrice(costPrice != null ? costPrice : (int)(price * 0.9));
        product.setStock(stock);
        product.setDescription(description);

        if (categoryId != null) {
            categoryRepository.findById(categoryId).ifPresent(product::setCategory);
        }
        if (providerId != null) {
            providerRepository.findById(providerId).ifPresent(product::setProvider);
        }

        Product saved = productRepository.save(product);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Integer id) {
        productRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Produk berhasil dihapus."));
    }

    // --- Voucher Management ---
    @Transactional(readOnly = true)
    @GetMapping("/vouchers")
    public ResponseEntity<List<Map<String, Object>>> getAllVouchers() {
        List<Voucher> vouchers = voucherRepository.findAll();
        List<Map<String, Object>> result = vouchers.stream().map(v -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", v.getId());
            map.put("code", v.getCode());
            map.put("discountType", v.getDiscountType());
            map.put("discountValue", v.getDiscountValue());
            map.put("minPurchase", v.getMinPurchase());
            map.put("maxDiscount", v.getMaxDiscount());
            map.put("usageLimit", v.getUsageLimit());
            map.put("usedCount", v.getUsedCount());
            map.put("startAt", v.getStartAt());
            map.put("endAt", v.getEndAt());
            map.put("active", v.isActive());
            map.put("stock", v.getStock());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/vouchers")
    public ResponseEntity<?> saveVoucher(@RequestBody Map<String, Object> body) {
        Integer id = (Integer) body.get("id");
        String code = (String) body.get("code");
        String discountType = (String) body.get("discountType");
        Number discountValue = (Number) body.get("discountValue");
        Number minPurchaseNum = (Number) body.get("minPurchase");
        Number maxDiscountNum = (Number) body.get("maxDiscount");
        Number usageLimitNum = (Number) body.get("usageLimit");
        Number usedCountNum = (Number) body.get("usedCount");
        String startAtStr = (String) body.get("startAt");
        String endAtStr = (String) body.get("endAt");
        Boolean active = (Boolean) body.get("active");

        Voucher v = (id != null) ? voucherRepository.findById(id).orElse(new Voucher()) : new Voucher();
        v.setCode(code != null ? code.trim().toUpperCase() : "");
        v.setDiscountType(discountType != null ? discountType.toUpperCase() : "NOMINAL");
        v.setDiscountValue(discountValue != null ? discountValue.doubleValue() : 0.0);
        v.setMinPurchase(minPurchaseNum != null ? minPurchaseNum.doubleValue() : 0.0);
        v.setMaxDiscount(maxDiscountNum != null ? maxDiscountNum.doubleValue() : null);
        v.setUsageLimit(usageLimitNum != null ? usageLimitNum.intValue() : 100);
        v.setUsedCount(usedCountNum != null ? usedCountNum.intValue() : 0);
        if (startAtStr != null && !startAtStr.isBlank()) {
            v.setStartAt(LocalDateTime.parse(startAtStr));
        }
        if (endAtStr != null && !endAtStr.isBlank()) {
            v.setEndAt(LocalDateTime.parse(endAtStr));
        }
        v.setActive(active != null ? active : true);

        Voucher saved = voucherRepository.save(v);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/vouchers/{id}")
    public ResponseEntity<?> deleteVoucher(@PathVariable Integer id) {
        voucherRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Voucher berhasil dihapus."));
    }

    // --- Flash Sale Management ---
    @Transactional(readOnly = true)
    @GetMapping("/flash-sales")
    public ResponseEntity<List<FlashSale>> getAllFlashSales() {
        return ResponseEntity.ok(flashSaleRepository.findAll());
    }

    @PostMapping("/flash-sales")
    public ResponseEntity<?> saveFlashSale(@RequestBody Map<String, Object> body) {
        Integer id = (Integer) body.get("id");
        Integer productId = (Integer) body.get("productId");
        Integer flashPrice = (Integer) body.get("flashPrice");
        String startAtStr = (String) body.get("startAt");
        String endAtStr = (String) body.get("endAt");
        Integer quota = (Integer) body.get("quota");

        if (productId == null || flashPrice == null || startAtStr == null || endAtStr == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Produk, harga flash sale, dan periode wajib diisi."));
        }
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Produk tidak ditemukan."));
        }
        Product product = productOpt.get();
        LocalDateTime startAt = LocalDateTime.parse(startAtStr);
        LocalDateTime endAt = LocalDateTime.parse(endAtStr);

        if (!endAt.isAfter(startAt)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Waktu selesai harus setelah waktu mulai."));
        }
        if (flashPrice <= 0 || (product.getPrice() != null && flashPrice >= product.getPrice())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Harga flash sale harus lebih dari 0 dan di bawah harga normal."));
        }
        if (quota != null && quota < 1) {
            return ResponseEntity.badRequest().body(Map.of("message", "Kuota minimal 1."));
        }
        // One product must not have two active flash sales in overlapping periods.
        if (flashSaleRepository.countOverlapping(productId, id != null ? id : -1, startAt, endAt) > 0) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    "Produk ini sudah punya flash sale aktif pada periode yang bertabrakan."));
        }

        FlashSale fs = (id != null) ? flashSaleRepository.findById(id).orElse(new FlashSale()) : new FlashSale();
        fs.setProduct(product);
        fs.setFlashPrice(flashPrice);
        fs.setStartAt(startAt);
        fs.setEndAt(endAt);
        fs.setQuota(quota != null ? quota : 100);
        fs.setActive(true);

        FlashSale saved = flashSaleRepository.save(fs);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/flash-sales/{id}")
    public ResponseEntity<?> deleteFlashSale(@PathVariable Integer id) {
        flashSaleRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Flash Sale berhasil dihapus."));
    }

    // --- Top Up Management ---
    // Mapped by hand: the entity would expose the owner's password hash and OTP.
    @Transactional(readOnly = true)
    @GetMapping("/topups")
    public ResponseEntity<Map<String, Object>> getAllTopups(@RequestParam(required = false) String status,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        TransactionStatus filter = null;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            try {
                filter = TransactionStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("message", "Status tidak dikenal."));
            }
        }
        Page<TopUp> result = filter == null
                ? topUpRepository.findAllByOrderByDateDesc(pageable)
                : topUpRepository.findByStatusOrderByDateDesc(filter, pageable);

        List<Map<String, Object>> items = result.getContent().stream().map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("username", t.getUser() != null ? t.getUser().getUsername() : "-");
            m.put("amount", t.getAmount());
            m.put("status", t.getStatus());
            m.put("method", t.getExternalId() == null ? "MANUAL" : "XENDIT");
            m.put("externalId", t.getExternalId());
            m.put("paymentMethod", t.getPaymentMethod());
            m.put("paymentChannel", t.getPaymentChannel());
            m.put("date", t.getDate());
            m.put("paidAt", t.getPaidAt());
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> resp = pageResponse(result, items);
        resp.put("pendingManual", topUpRepository.countByStatusAndExternalIdIsNull(TransactionStatus.PENDING));
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/topups/{id}/approve")
    public ResponseEntity<?> approveManualTopup(@PathVariable Integer id) {
        try {
            topUpService.approveManual(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Top up manual berhasil disetujui."));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/topups/{id}/reject")
    public ResponseEntity<?> rejectManualTopup(@PathVariable Integer id) {
        try {
            topUpService.rejectManual(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Top up manual ditolak."));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // --- User Management ---
    // Mapped by hand: never send password hashes or OTP codes to the browser.
    // Mapped by hand: never send password hashes or OTP codes to the browser.
    @Transactional(readOnly = true)
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getAllUsers(@RequestParam(required = false) String search,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        String q = search == null || search.isBlank() ? "%" : "%" + search.trim().toLowerCase() + "%";
        Page<User> result = userRepository.search(q, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
        List<Map<String, Object>> items = result.getContent().stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("email", u.getEmail());
            m.put("phoneNumber", u.getPhoneNumber());
            m.put("role", u.getRole());
            m.put("status", u.getStatus());
            m.put("balance", u.getBalance() != null ? u.getBalance() : 0);
            m.put("emailVerified", u.isEmailVerified());
            m.put("authProvider", u.getAuthProvider());
            m.put("createdAt", u.getCreatedAt());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(pageResponse(result, items));
    }

    private static Map<String, Object> pageResponse(Page<?> page, List<Map<String, Object>> items) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("items", items);
        resp.put("page", page.getNumber());
        resp.put("totalPages", page.getTotalPages());
        resp.put("totalElements", page.getTotalElements());
        return resp;
    }

    @PostMapping("/users/{id}/toggle-ban")
    public ResponseEntity<?> toggleBanUser(@PathVariable Integer id) {
        Optional<User> uOpt = userRepository.findById(id);
        if (uOpt.isEmpty()) return ResponseEntity.notFound().build();

        User u = uOpt.get();
        if ("ROLE_ADMIN".equals(u.getRole())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Akun admin tidak bisa diblokir."));
        }
        if ("ACTIVE".equalsIgnoreCase(u.getStatus())) {
            userService.banUser(id);
            return ResponseEntity.ok(Map.of("success", true, "status", "BANNED", "message", "Akun berhasil dinonaktifkan."));
        } else {
            userService.unbanUser(id);
            return ResponseEntity.ok(Map.of("success", true, "status", "ACTIVE", "message", "Akun berhasil diaktifkan."));
        }
    }

    // --- Category Management ---
    @PostMapping("/categories")
    public ResponseEntity<?> saveCategory(@RequestBody Map<String, String> body) {
        String id = body.get("id");
        String code = body.get("code");
        String type = body.get("type");
        if (type == null || type.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nama kategori wajib diisi."));
        }
        String normalized = type.trim().toUpperCase();
        if (categoryRepository.findAll().stream().anyMatch(c -> normalized.equals(c.getType()) && !c.getId().equals(id))) {
            return ResponseEntity.badRequest().body(Map.of("message", "Kategori " + normalized + " sudah ada."));
        }
        Category category = (id != null && !id.isBlank())
                ? categoryRepository.findById(id).orElse(new Category())
                : new Category();
        category.setCode(code != null && !code.isBlank() ? code.trim() : "CD-" + System.currentTimeMillis());
        category.setType(normalized);
        return ResponseEntity.ok(categoryRepository.save(category));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable String id) {
        Integer count = productRepository.countByCategoryId(id);
        if (count != null && count > 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Kategori sedang digunakan oleh " + count + " produk."));
        }
        categoryRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Kategori berhasil dihapus."));
    }

    // --- Provider Management ---
    @PostMapping("/providers")
    public ResponseEntity<?> saveProvider(@RequestBody Map<String, Object> body) {
        Integer id = (Integer) body.get("id");
        String name = body.get("name") instanceof String s ? s.trim() : "";
        if (name.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nama provider wajib diisi."));
        }
        // The DB unique constraint is case-sensitive; "telkomsel" must not duplicate "Telkomsel".
        if (providerRepository.findAll().stream().anyMatch(p -> name.equalsIgnoreCase(p.getName()) && !p.getId().equals(id))) {
            return ResponseEntity.badRequest().body(Map.of("message", "Provider " + name + " sudah ada."));
        }
        Provider provider = (id != null)
                ? providerRepository.findById(id).orElse(new Provider())
                : new Provider();
        provider.setName(name);
        try {
            return ResponseEntity.ok(providerRepository.saveAndFlush(provider));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Provider dengan nama itu sudah ada."));
        }
    }

    @DeleteMapping("/providers/{id}")
    public ResponseEntity<?> deleteProvider(@PathVariable Integer id) {
        long count = productRepository.countByProviderId(id);
        if (count > 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Provider sedang digunakan oleh " + count + " produk."));
        }
        providerRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Provider berhasil dihapus."));
    }
}
