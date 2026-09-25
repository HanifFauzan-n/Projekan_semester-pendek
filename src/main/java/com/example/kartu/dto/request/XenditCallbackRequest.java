package com.example.kartu.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;

/**
 * Body webhook yang dikirim Xendit ketika status invoice berubah (SKPL-F05).
 *
 * PERINGATAN KEAMANAN: isi objek ini datang dari luar sistem dan belum tentu
 * berasal dari Xendit. Jangan pernah menambah saldo hanya berdasarkan isi
 * objek ini. Wajib lebih dulu:
 *   1. memverifikasi header X-CALLBACK-TOKEN,
 *   2. memastikan top up dengan externalId tersebut belum berstatus lunas,
 *   3. mencocokkan paidAmount dengan nominal top up yang tersimpan.
 *
 * paidAt sengaja dibiarkan bertipe String supaya perbedaan format tanggal
 * tidak membuat parsing webhook gagal. Gunakan parsePaidAt() untuk mengubahnya
 * menjadi Instant; kalau formatnya tidak dikenali, metode itu mengembalikan null
 * dan pemroses dapat memakai waktu server sebagai gantinya.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class XenditCallbackRequest {

    private String id;
    private String externalId;
    private String userId;
    private String status;
    private String merchantName;
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private String payerEmail;
    private String description;
    private String paymentMethod;
    private String paymentChannel;
    private String paymentDestination;
    private String currency;
    private String paidAt;
    private String created;
    private String updated;

    /** true kalau invoice benar-benar sudah dibayar. */
    public boolean isPaid() {
        return "PAID".equalsIgnoreCase(status) || "SETTLED".equalsIgnoreCase(status);
    }

    /** true kalau invoice kedaluwarsa tanpa dibayar. */
    public boolean isExpired() {
        return "EXPIRED".equalsIgnoreCase(status);
    }

    /**
     * Mengubah paidAt menjadi Instant. Mengembalikan null kalau kosong atau
     * formatnya tidak dikenali, bukan melempar exception, supaya satu webhook
     * dengan format tanggal aneh tidak menggagalkan pencatatan pembayaran.
     */
    public java.time.Instant parsePaidAt() {
        if (paidAt == null || paidAt.isBlank()) {
            return null;
        }
        try {
            return java.time.Instant.parse(paidAt);
        } catch (java.time.format.DateTimeParseException ignored) {
            // lanjut ke percobaan berikutnya
        }
        try {
            return java.time.OffsetDateTime.parse(paidAt).toInstant();
        } catch (java.time.format.DateTimeParseException ignored) {
            return null;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public String getPayerEmail() {
        return payerEmail;
    }

    public void setPayerEmail(String payerEmail) {
        this.payerEmail = payerEmail;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentChannel() {
        return paymentChannel;
    }

    public void setPaymentChannel(String paymentChannel) {
        this.paymentChannel = paymentChannel;
    }

    public String getPaymentDestination() {
        return paymentDestination;
    }

    public void setPaymentDestination(String paymentDestination) {
        this.paymentDestination = paymentDestination;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(String paidAt) {
        this.paidAt = paidAt;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getUpdated() {
        return updated;
    }

    public void setUpdated(String updated) {
        this.updated = updated;
    }
}
