package com.example.kartu.models;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A product paid directly through a Xendit invoice instead of the balance (docs/sql/010).
 * PENDING until the PAID webhook; PAID while the product is being processed; then SUCCESS
 * (transactionId points at transaction_history) or FAILED (money returned to the balance).
 * EXPIRED when the invoice lapses unpaid.
 */
@Entity
@Data
@NoArgsConstructor
@Table(name = "product_orders")
public class ProductOrder {

    public enum Status { PENDING, PAID, SUCCESS, FAILED, EXPIRED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true, updatable = false)
    private String externalId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "customer_number", nullable = false)
    private String customerNumber;

    @Column(name = "voucher_code")
    private String voucherCode;

    /** Product price after flash sale and voucher, quoted at checkout. */
    private Integer price;

    @Column(name = "admin_fee")
    private Integer adminFee;

    /** price + adminFee: the amount of the Xendit invoice. */
    private Integer total;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    private String note;

    @Column(name = "invoice_url")
    private String invoiceUrl;

    @Column(name = "xendit_invoice_id")
    private String xenditInvoiceId;

    @Column(name = "payment_channel")
    private String paymentChannel;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
