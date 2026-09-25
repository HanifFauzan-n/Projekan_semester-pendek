package com.example.kartu.services;

import com.example.kartu.config.XenditProperties;
import com.example.kartu.dto.request.XenditInvoiceRequest;
import com.example.kartu.dto.response.XenditInvoiceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * Pembungkus REST API Xendit (SKPL-F05).
 *
 * Kelas ini hanya berurusan dengan komunikasi HTTP ke Xendit dan verifikasi
 * token webhook. Logika bisnis top up (membuat record, menambah saldo,
 * menjaga agar saldo tidak bertambah dua kali) berada di PaymentService,
 * bukan di sini.
 *
 * Autentikasi Xendit memakai HTTP Basic dengan secret API key sebagai username
 * dan password dikosongkan.
 */
@Service
public class XenditService {

    private static final Logger log = LoggerFactory.getLogger(XenditService.class);
    private static final String CREATE_INVOICE_PATH = "/v2/invoices";

    private final XenditProperties properties;
    private final RestClient restClient;

    public XenditService(XenditProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeaders(headers -> headers.setBasicAuth(properties.getApiKey(), ""))
                .build();
    }

    /**
     * Membuat external_id unik untuk satu permintaan top up.
     * Format: topup-{idTopUp}-{8 karakter acak}. Bagian acak mencegah tabrakan
     * kalau satu record top up sempat dibuatkan invoice lebih dari sekali.
     */
    public String buildExternalId(Long topUpId) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return "topup-" + topUpId + "-" + suffix;
    }

    /**
     * Membuat invoice di Xendit dan mengembalikan responsnya.
     * Invoice ini menyediakan virtual account, QRIS, dan e-wallet sekaligus
     * pada satu halaman pembayaran milik Xendit.
     *
     * @param externalId         id unik dari sistem, dipakai untuk mencocokkan webhook
     * @param amount             nominal top up
     * @param payerEmail         email pelanggan, boleh null
     * @param description        keterangan yang tampil di halaman pembayaran
     * @param successRedirectUrl tujuan setelah pembayaran berhasil
     * @param failureRedirectUrl tujuan setelah pembayaran gagal
     * @throws XenditException kalau API key belum diisi atau Xendit membalas error
     */
    public XenditInvoiceResponse createInvoice(String externalId,
                                               BigDecimal amount,
                                               String payerEmail,
                                               String description,
                                               String successRedirectUrl,
                                               String failureRedirectUrl) {
        requireConfigured();

        XenditInvoiceRequest request = new XenditInvoiceRequest(externalId, amount);
        request.setPayerEmail(payerEmail);
        request.setDescription(description);
        request.setSuccessRedirectUrl(successRedirectUrl);
        request.setFailureRedirectUrl(failureRedirectUrl);
        request.setInvoiceDuration(properties.getInvoiceDurationSeconds());

        log.info("Membuat invoice Xendit externalId={} nominal={}", externalId, amount);

        XenditInvoiceResponse response = restClient.post()
                .uri(CREATE_INVOICE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    String body = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    log.error("Xendit menolak pembuatan invoice externalId={} status={} body={}",
                            externalId, res.getStatusCode(), body);
                    throw new XenditException(
                            "Xendit menolak permintaan pembuatan invoice (HTTP "
                                    + res.getStatusCode().value() + "): " + body);
                })
                .body(XenditInvoiceResponse.class);

        if (response == null || response.getInvoiceUrl() == null) {
            throw new XenditException("Xendit membalas tanpa invoice_url untuk externalId " + externalId);
        }

        log.info("Invoice Xendit dibuat externalId={} invoiceId={}", externalId, response.getId());
        return response;
    }

    /**
     * Memverifikasi header X-CALLBACK-TOKEN dari webhook Xendit.
     *
     * Ini satu-satunya hal yang membedakan webhook asli dari kiriman siapa pun
     * yang kebetulan tahu alamat endpoint callback. Tanpa pemeriksaan ini,
     * orang lain bisa mengirim callback palsu dan menambah saldo sendiri.
     *
     * Perbandingan memakai MessageDigest.isEqual agar waktu eksekusinya tidak
     * bergantung pada berapa banyak karakter awal yang cocok.
     *
     * @return true hanya kalau token dikonfigurasi dan persis sama
     */
    public boolean isValidCallbackToken(String tokenFromHeader) {
        String expected = properties.getCallbackToken();
        if (expected == null || expected.isBlank()) {
            log.error("xendit.callback-token belum diisi. Semua webhook ditolak "
                    + "agar saldo tidak bisa ditambah oleh pihak yang tidak berwenang.");
            return false;
        }
        if (tokenFromHeader == null || tokenFromHeader.isBlank()) {
            log.warn("Webhook masuk tanpa header X-CALLBACK-TOKEN, ditolak.");
            return false;
        }
        boolean valid = MessageDigest.isEqual(
                tokenFromHeader.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
        if (!valid) {
            log.warn("Webhook masuk dengan X-CALLBACK-TOKEN yang tidak cocok, ditolak.");
        }
        return valid;
    }

    private void requireConfigured() {
        if (!properties.isConfigured()) {
            throw new XenditException("XENDIT_API_KEY belum diisi. "
                    + "Isi di file .env pada root proyek sebelum memakai pembayaran otomatis.");
        }
    }

    /** Dilempar kalau integrasi Xendit gagal. Ditangani di lapisan controller. */
    public static class XenditException extends RuntimeException {
        public XenditException(String message) {
            super(message);
        }

        public XenditException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
