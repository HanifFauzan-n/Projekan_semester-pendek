package com.example.kartu.services;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.kartu.dto.request.XenditCallbackRequest;
import com.example.kartu.dto.response.XenditInvoiceResponse;
import com.example.kartu.enums.PurchaseTarget;
import com.example.kartu.models.Product;
import com.example.kartu.models.ProductOrder;
import com.example.kartu.models.ProductOrder.Status;
import com.example.kartu.models.TransactionHistory;
import com.example.kartu.models.User;
import com.example.kartu.repositories.ProductOrderRepository;
import com.example.kartu.repositories.ProductRepository;
import com.example.kartu.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Buying a product without balance: the customer pays a Xendit invoice of price + 10% admin fee.
 *
 * When the PAID webhook arrives the order flips PENDING -> PAID and the product price is credited
 * to the customer's balance in the same transaction; the normal purchase then runs and debits it,
 * so stock, flash sale quota and voucher quota stay atomic (CLAUDE.md rule 7). If the purchase
 * fails (sold out, voucher gone...) the whole payment, admin fee included, stays in the balance.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    public static final String EXTERNAL_ID_PREFIX = "order-";
    /** Admin fee for paying a product directly through Xendit. */
    public static final double XENDIT_FEE_RATE = 0.10;

    private static final Locale ID = Locale.forLanguageTag("id-ID");

    private final ProductOrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final TransactionHistoryService transactionHistoryService;
    private final XenditService xenditService;
    private final TransactionTemplate transactionTemplate;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public static int adminFee(int price) {
        return (int) Math.round(price * XENDIT_FEE_RATE);
    }

    /** Creates a PENDING order and its Xendit invoice. Nothing is saved when Xendit refuses. */
    @Transactional(rollbackFor = Exception.class)
    public ProductOrder createXenditOrder(User user, Integer productId, String voucherCode, String customerNumber)
            throws Exception {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new Exception("Produk tidak ditemukan."));
        PurchaseTarget target = PurchaseTarget.of(product);
        if (target == null) {
            throw new Exception("Produk '" + product.getName() + "' hanya dijual langsung di konter Zelatan Cell.");
        }
        String destination = target.normalize(customerNumber);
        if (product.getStock() == null || product.getStock() <= 0) {
            throw new Exception("Stok produk '" + product.getName() + "' sudah habis.");
        }

        int price = transactionHistoryService.quote(product, user, voucherCode);
        ProductOrder order = new ProductOrder();
        order.setExternalId(EXTERNAL_ID_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        order.setUser(user);
        order.setProduct(product);
        order.setCustomerNumber(destination);
        order.setVoucherCode(voucherCode == null || voucherCode.isBlank() ? null : voucherCode.trim().toUpperCase());
        order.setPrice(price);
        order.setAdminFee(adminFee(price));
        order.setTotal(price + order.getAdminFee());
        orderRepository.save(order);

        String returnUrl = frontendUrl + "/pesanan/" + order.getExternalId();
        XenditInvoiceResponse invoice = xenditService.createInvoice(
                order.getExternalId(),
                BigDecimal.valueOf(order.getTotal()),
                user.getEmail() != null && !user.getEmail().isBlank() ? user.getEmail() : null,
                product.getName() + " ke " + destination + " (termasuk biaya admin 10%)",
                returnUrl,
                returnUrl);
        order.setXenditInvoiceId(invoice.getId());
        order.setInvoiceUrl(invoice.getInvoiceUrl());
        return order;
    }

    /** Webhook for an order invoice; the controller has already verified X-CALLBACK-TOKEN. Never throws. */
    public void handleCallback(XenditCallbackRequest callback) {
        ProductOrder order = orderRepository.findByExternalId(callback.getExternalId()).orElse(null);
        if (order == null) {
            log.warn("Webhook Xendit untuk pesanan {} yang tidak dikenal, diabaikan.", callback.getExternalId());
            return;
        }
        if (callback.isExpired()) {
            orderRepository.transition(order.getId(), Status.PENDING, Status.EXPIRED,
                    "Invoice kedaluwarsa sebelum dibayar.", null);
            return;
        }
        if (!callback.isPaid()) {
            return;
        }
        BigDecimal paid = callback.getPaidAmount();
        if (paid == null || paid.compareTo(BigDecimal.valueOf(order.getTotal())) != 0) {
            // left PENDING for an admin to review instead of crediting a wrong amount
            log.error("Nominal webhook pesanan {} tidak cocok: tagihan={} dibayar={}.",
                    order.getExternalId(), order.getTotal(), paid);
            return;
        }

        Integer userId = order.getUser().getId();
        OffsetDateTime paidAt = callback.parsePaidAt() == null
                ? OffsetDateTime.now() : callback.parsePaidAt().atOffset(ZoneOffset.UTC);
        Boolean claimed = transactionTemplate.execute(status -> {
            if (orderRepository.markPaid(order.getId(), paidAt, callback.getPaymentChannel()) == 0) {
                return false;
            }
            userRepository.creditBalance(userId, order.getPrice());
            return true;
        });
        if (!Boolean.TRUE.equals(claimed)) {
            log.info("Webhook pesanan {} sudah pernah diproses, diabaikan.", order.getExternalId());
            return;
        }

        try {
            TransactionHistory tx = transactionHistoryService.purchaseProduct(order.getProduct().getId(),
                    order.getUser().getUsername(), order.getVoucherCode(), order.getCustomerNumber(),
                    "XENDIT", order.getAdminFee(), order.getPrice());
            orderRepository.transition(order.getId(), Status.PAID, Status.SUCCESS, null, tx.getTransactionId());
            log.info("Pesanan {} lunas dan diproses sebagai {}.", order.getExternalId(), tx.getTransactionId());
        } catch (Exception e) {
            String note = e.getMessage() + String.format(ID,
                    " Pembayaran Rp %,d sudah dikembalikan ke saldo Anda dan bisa dipakai untuk membeli produk lain.",
                    order.getTotal());
            transactionTemplate.executeWithoutResult(status -> {
                orderRepository.transition(order.getId(), Status.PAID, Status.FAILED, note, null);
                userRepository.creditBalance(userId, order.getAdminFee());
            });
            log.warn("Pesanan {} dibayar tetapi gagal diproses ({}); dana dikembalikan ke saldo.",
                    order.getExternalId(), e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public java.util.Optional<ProductOrder> findForUser(String externalId, User user) {
        return orderRepository.findByExternalId(externalId)
                .filter(o -> o.getUser().getId().equals(user.getId()));
    }
}
