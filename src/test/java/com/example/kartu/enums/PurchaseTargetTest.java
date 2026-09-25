package com.example.kartu.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.example.kartu.models.Category;
import com.example.kartu.models.Product;

class PurchaseTargetTest {

    private static Product productIn(String categoryType) {
        Product p = new Product();
        p.setCategory(new Category("X", categoryType));
        return p;
    }

    @Test
    void targetFollowsCategory() {
        assertEquals(PurchaseTarget.PHONE, PurchaseTarget.of(productIn("PULSA")));
        assertEquals(PurchaseTarget.PHONE, PurchaseTarget.of(productIn("PAKET DATA")));
        assertEquals(PurchaseTarget.PLN_METER, PurchaseTarget.of(productIn("TOKEN PLN")));
        assertEquals(PurchaseTarget.GAME_ID, PurchaseTarget.of(productIn("VOUCHER GAME")));
        assertNull(PurchaseTarget.of(productIn("ACCESSORIES")));
        assertNull(PurchaseTarget.of(new Product()));
    }

    @Test
    void phoneNumberIsNormalizedTo08() {
        assertEquals("081299990000", PurchaseTarget.PHONE.normalize("+62 812-9999-0000"));
        assertEquals("081299990000", PurchaseTarget.PHONE.normalize("6281299990000"));
        assertEquals("0812345678", PurchaseTarget.PHONE.normalize("0812345678"));
        assertThrows(IllegalArgumentException.class, () -> PurchaseTarget.PHONE.normalize(null));
        assertThrows(IllegalArgumentException.class, () -> PurchaseTarget.PHONE.normalize("12345"));
        assertThrows(IllegalArgumentException.class, () -> PurchaseTarget.PHONE.normalize("0212345678"));
        assertThrows(IllegalArgumentException.class, () -> PurchaseTarget.PHONE.normalize("08123456789012"));
    }

    @Test
    void plnMeterIs11Or12Digits() {
        assertEquals("14234567890", PurchaseTarget.PLN_METER.normalize("1423 4567 890"));
        assertEquals("542100123456", PurchaseTarget.PLN_METER.normalize("542100123456"));
        assertThrows(IllegalArgumentException.class, () -> PurchaseTarget.PLN_METER.normalize("1234567890"));
        assertThrows(IllegalArgumentException.class, () -> PurchaseTarget.PLN_METER.normalize("1234567890123"));
        assertThrows(IllegalArgumentException.class, () -> PurchaseTarget.PLN_METER.normalize("1423456789a"));
    }

    @Test
    void gameIdAllowsCommonFormats() {
        assertEquals("12345678 (1234)", PurchaseTarget.GAME_ID.normalize("  12345678   (1234) "));
        assertEquals("Nama#TAG", PurchaseTarget.GAME_ID.normalize("Nama#TAG"));
        assertThrows(IllegalArgumentException.class, () -> PurchaseTarget.GAME_ID.normalize(null));
        assertThrows(IllegalArgumentException.class, () -> PurchaseTarget.GAME_ID.normalize("123"));
        assertThrows(IllegalArgumentException.class, () -> PurchaseTarget.GAME_ID.normalize("<script>"));
    }
}
