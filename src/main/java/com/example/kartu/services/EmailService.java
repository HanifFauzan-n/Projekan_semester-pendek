package com.example.kartu.services;

import com.example.kartu.enums.PurchaseTarget;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.util.HtmlUtils;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Email notifications (SKPL-F18): OTP, welcome, top up success, purchase receipt.
 *
 * Rules: sending never breaks the business flow (every failure is only logged), runs on
 * a background thread (@Async), and notifications about money are sent only after the
 * database transaction commits (see {@link #afterCommit(Runnable)}), so a rolled back
 * purchase never produces a receipt.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    // RFC 2606 reserved names used by seed and test accounts: never deliverable, skip them
    // so development does not fill the sender's inbox with bounces.
    private static final List<String> RESERVED_DOMAINS = List.of(".test", ".local", ".example", ".invalid", ".localhost");
    private static final Locale ID = Locale.forLanguageTag("id-ID");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("d MMMM yyyy, HH.mm 'WIB'", ID);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@zelatancell.local}")
    private String fromEmail;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    /** Runs the action after the current transaction commits, or now when there is none. */
    public static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    @Async
    public void sendVerificationOtp(String toEmail, String username, String otp) {
        if (skip(toEmail)) {
            // Local development without SMTP: the code is only visible in the server log.
            log.info("[DEV, email nonaktif] OTP VERIFIKASI AKUN untuk {} ({}): {}", username, toEmail, otp);
            return;
        }
        sendHtmlMail(toEmail, "[Zelatan Cell] Kode Verifikasi Pendaftaran Akun", layout(username,
                "Terima kasih telah mendaftar di Zelatan Cell. Berikut adalah kode One-Time Password (OTP) Anda untuk memverifikasi alamat email dan mengaktifkan akun:",
                otpCard(otp),
                "Kode OTP ini berlaku selama 5 menit. Jangan bagikan kode ini kepada siapapun demi keamanan akun Anda."));
    }

    @Async
    public void sendPasswordResetOtp(String toEmail, String username, String otp) {
        if (skip(toEmail)) {
            log.info("[DEV, email nonaktif] OTP RESET PASSWORD untuk {} ({}): {}", username, toEmail, otp);
            return;
        }
        sendHtmlMail(toEmail, "[Zelatan Cell] Kode OTP Reset Password", layout(username,
                "Kami menerima permintaan untuk mereset kata sandi akun Zelatan Cell Anda. Gunakan kode OTP di bawah ini untuk melanjutkan:",
                otpCard(otp),
                "Kode OTP ini berlaku selama 5 menit. Jika Anda tidak merasa meminta reset password, abaikan email ini."));
    }

    /** Registration completed (after OTP verification or first Google login). */
    @Async
    public void sendWelcome(String toEmail, String username) {
        if (skip(toEmail)) {
            log.info("[DEV, email nonaktif] Email selamat datang untuk {} ({})", username, toEmail);
            return;
        }
        sendHtmlMail(toEmail, "[Zelatan Cell] Selamat datang di Zelatan Cell", layout(username,
                "Akun Zelatan Cell Anda sudah aktif. Sekarang Anda bisa mengisi saldo dan membeli pulsa, paket data, token listrik, dan voucher game kapan saja.",
                detailTable(new String[][]{
                        {"Username", username},
                        {"Tanggal daftar", LocalDateTime.now().format(TIME)},
                        {"Masuk ke akun", frontendUrl + "/login"},
                }),
                "Simpan email ini sebagai bukti pendaftaran."));
    }

    @Async
    public void sendTopUpSuccess(String toEmail, String username, long amount, String method,
                                 long newBalance, String reference, LocalDateTime time) {
        if (skip(toEmail)) {
            log.info("[DEV, email nonaktif] Top up berhasil {} ({}): {} via {}, saldo {}", username, toEmail, amount, method, newBalance);
            return;
        }
        sendHtmlMail(toEmail, "[Zelatan Cell] Top up saldo berhasil", layout(username,
                "Top up saldo Anda sudah masuk. Berikut rinciannya:",
                detailTable(new String[][]{
                        {"Nominal top up", rupiah(amount)},
                        {"Metode", method},
                        {"Saldo sekarang", rupiah(newBalance)},
                        {"Waktu", time.format(TIME)},
                        {"ID transaksi", reference},
                }),
                "Jika Anda tidak merasa melakukan top up ini, segera hubungi Zelatan Cell."));
    }

    @Async
    public void sendPurchaseReceipt(String toEmail, String username, String productName, PurchaseTarget target,
                                    String customerNumber, long price, long discount,
                                    String voucherCode, long total, String paymentMethod, long adminFee,
                                    long newBalance, String transactionId, String serialNumber, LocalDateTime time) {
        boolean xendit = "XENDIT".equals(paymentMethod);
        if (skip(toEmail)) {
            log.info("[DEV, email nonaktif] Struk pembelian {} ({}): {} {} ke {} total {} via {}", username, toEmail,
                    transactionId, productName, customerNumber, total + adminFee, paymentMethod);
            return;
        }
        List<String[]> rows = new ArrayList<>(List.of(
                new String[]{"Produk", productName},
                new String[]{target.numberLabel, customerNumber},
                new String[]{"Harga", rupiah(price)},
                new String[]{"Diskon" + (voucherCode != null ? " (voucher " + voucherCode + ")" : ""), "- " + rupiah(discount)}));
        if (xendit) {
            rows.add(new String[]{"Biaya admin Xendit", rupiah(adminFee)});
        }
        rows.add(new String[]{"Total bayar", rupiah(total + adminFee)});
        rows.add(new String[]{"Metode bayar", xendit ? "Xendit" : "Saldo Zelatan Cell"});
        if (!xendit) {
            rows.add(new String[]{"Sisa saldo", rupiah(newBalance)});
        }
        rows.add(new String[]{"Waktu", time.format(TIME)});
        rows.add(new String[]{"ID transaksi", transactionId});
        rows.add(new String[]{target.codeLabel, target == PurchaseTarget.PLN_METER ? groupTokenDigits(serialNumber) : serialNumber});
        sendHtmlMail(toEmail, "[Zelatan Cell] Struk pembelian " + transactionId, layout(username,
                "Terima kasih, pembelian Anda berhasil. Berikut struk transaksinya:",
                detailTable(rows.toArray(String[][]::new)),
                "Simpan email ini sebagai bukti pembelian yang sah."));
    }

    private boolean skip(String toEmail) {
        if (!mailEnabled || toEmail == null || toEmail.isBlank()) {
            return true;
        }
        String lower = toEmail.trim().toLowerCase(Locale.ROOT);
        return RESERVED_DOMAINS.stream().anyMatch(lower::endsWith);
    }

    private void sendHtmlMail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email \"{}\" berhasil dikirim ke {}", subject, to);
        } catch (Exception e) {
            log.warn("Gagal mengirim email \"{}\" ke {}: {}", subject, to, e.getMessage());
        }
    }

    private static String rupiah(long value) {
        return "Rp " + NumberFormat.getNumberInstance(ID).format(value);
    }

    /** 20-digit PLN token in groups of four, as printed on PLN receipts. */
    private static String groupTokenDigits(String token) {
        return token.replaceAll("(\\d{4})(?=\\d)", "$1-");
    }

    private static String otpCard(String otp) {
        return """
                <div class="otp-card">
                  <div class="otp-label">Kode Verifikasi OTP</div>
                  <div class="otp-code">%s</div>
                </div>""".formatted(HtmlUtils.htmlEscape(otp));
    }

    private static String detailTable(String[][] rows) {
        StringBuilder sb = new StringBuilder("<table class=\"detail\">");
        for (String[] row : rows) {
            sb.append("<tr><td class=\"k\">").append(HtmlUtils.htmlEscape(row[0])).append("</td><td class=\"v\">")
                    .append(HtmlUtils.htmlEscape(row[1] == null ? "-" : row[1])).append("</td></tr>");
        }
        return sb.append("</table>").toString();
    }

    /** Shared layout. {@code body} must already be escaped HTML; other arguments are escaped here. */
    private static String layout(String name, String intro, String body, String note) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f8fafc; margin: 0; padding: 0; }
                .container { max-width: 540px; margin: 30px auto; background: #ffffff; border-radius: 16px; border: 1px solid #e2e8f0; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); }
                .header { background: linear-gradient(135deg, #2563eb 0%%, #4f46e5 100%%); padding: 30px; text-align: center; color: #ffffff; }
                .header h1 { margin: 0; font-size: 24px; font-weight: 800; letter-spacing: -0.5px; }
                .header p { margin: 6px 0 0; font-size: 12px; opacity: 0.85; text-transform: uppercase; letter-spacing: 1px; }
                .content { padding: 30px; color: #1e293b; line-height: 1.6; }
                .greeting { font-size: 16px; font-weight: 700; margin-bottom: 12px; }
                .message { font-size: 14px; color: #475569; margin-bottom: 24px; }
                .otp-card { background: #f1f5f9; border: 2px dashed #94a3b8; border-radius: 12px; padding: 20px; text-align: center; margin-bottom: 24px; }
                .otp-label { font-size: 11px; text-transform: uppercase; font-weight: 700; color: #64748b; letter-spacing: 1.5px; }
                .otp-code { font-family: 'Courier New', monospace; font-size: 36px; font-weight: 900; color: #1e40af; letter-spacing: 8px; margin: 8px 0; }
                .detail { width: 100%%; border-collapse: collapse; font-size: 14px; margin-bottom: 24px; }
                .detail td { padding: 8px 0; border-bottom: 1px solid #f1f5f9; }
                .detail .k { color: #64748b; }
                .detail .v { text-align: right; font-weight: 700; color: #0f172a; }
                .note { font-size: 12px; color: #64748b; margin-top: 16px; padding: 12px; background: #fffbeb; border-radius: 8px; border-left: 4px solid #f59e0b; }
                .footer { background: #f8fafc; padding: 20px; text-align: center; font-size: 11px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>Zelatan Cell</h1>
                  <p>Layanan Digital & Pulsa</p>
                </div>
                <div class="content">
                  <div class="greeting">Halo, %s!</div>
                  <div class="message">%s</div>
                  %s
                  <div class="note">%s</div>
                </div>
                <div class="footer">
                  © 2026 Zelatan Cell. Jl. Temugiri 01 Tinggarjati Lor, Desa Gentasari, Kroya, Cilacap, Jawa Tengah.
                </div>
              </div>
            </body>
            </html>
            """.formatted(name != null ? HtmlUtils.htmlEscape(name) : "Pelanggan",
                HtmlUtils.htmlEscape(intro), body, HtmlUtils.htmlEscape(note));
    }
}
