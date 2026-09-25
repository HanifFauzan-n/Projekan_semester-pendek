package com.example.kartu.enums;

import java.util.Locale;

import com.example.kartu.models.Product;

/**
 * Where a digital product is delivered: a phone number (pulsa, paket data), a PLN meter
 * (token listrik) or a game account (voucher game). Decided by the category name so
 * admin-created categories keep working. Products without a target are physical goods
 * (accessories): they are sold at the counter only, not through the app.
 */
public enum PurchaseTarget {
    PHONE("Nomor HP tujuan", "Serial number"),
    PLN_METER("Nomor meter / ID pelanggan", "Token listrik"),
    GAME_ID("ID game", "Serial number");

    public final String numberLabel;
    public final String codeLabel;

    PurchaseTarget(String numberLabel, String codeLabel) {
        this.numberLabel = numberLabel;
        this.codeLabel = codeLabel;
    }

    public static PurchaseTarget of(Product product) {
        if (product == null || product.getCategory() == null || product.getCategory().getType() == null) {
            return null;
        }
        String type = product.getCategory().getType().toUpperCase(Locale.ROOT);
        if (type.contains("GAME")) {
            return GAME_ID;
        }
        if (type.contains("PLN") || type.contains("LISTRIK")) {
            return PLN_METER;
        }
        if (type.contains("CREDIT") || type.contains("PULSA") || type.contains("DATA") || type.contains("KUOTA")) {
            return PHONE;
        }
        return null;
    }

    /** Returns the number in canonical form (phone: 08xx), or throws with a message for the customer. */
    public String normalize(String raw) {
        if (this == GAME_ID) {
            // e.g. Mobile Legends "12345678 (1234)", Free Fire "123456789", Valorant "Nama#TAG"
            String id = raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
            if (!id.matches("[\\p{L}\\p{N} #()._-]{4,40}")) {
                throw new IllegalArgumentException("ID game tidak valid. Contoh Mobile Legends: 12345678 (1234).");
            }
            return id;
        }
        String n = raw == null ? "" : raw.replaceAll("[\\s.-]", "");
        if (this == PHONE) {
            if (n.startsWith("+62")) {
                n = "0" + n.substring(3);
            } else if (n.startsWith("62")) {
                n = "0" + n.substring(2);
            }
            // Indonesian mobile numbers: 08, then 8-11 more digits (10-13 digits in total)
            if (!n.matches("08[1-9]\\d{7,10}")) {
                throw new IllegalArgumentException("Nomor HP tujuan tidak valid. Contoh: 081234567890.");
            }
        } else if (!n.matches("\\d{11,12}")) {
            throw new IllegalArgumentException("Nomor meter / ID pelanggan PLN harus 11 atau 12 digit angka.");
        }
        return n;
    }

    /** Length of the code handed to the customer: PLN tokens are 20 digits. */
    public int codeLength() {
        return this == PLN_METER ? 20 : 16;
    }
}
