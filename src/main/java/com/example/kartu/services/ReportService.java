package com.example.kartu.services;

import com.example.kartu.models.FlashSale;
import com.example.kartu.models.Product;
import com.example.kartu.repositories.FlashSaleRepository;
import com.example.kartu.repositories.ProductRepository;
import com.example.kartu.repositories.TopUpRepository;
import com.example.kartu.repositories.TransactionHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Read-only report menu. Every number is aggregated by the database; callers (the admin
 * dashboard and the AI chat) only choose a report and a date range. The AI chat never
 * writes SQL: it can only pick a name from {@link #MENU}.
 *
 * Output keys are Indonesian on purpose: the maps are handed to the language model as-is.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    /** Report name -> description shown to the AI router. Order is the order in the prompt. */
    public static final Map<String, String> MENU = new LinkedHashMap<>();
    static {
        MENU.put("sales_summary", "omset, laba kotor, jumlah transaksi, total diskon, jumlah pelanggan yang bertransaksi");
        MENU.put("daily_sales", "omset dan jumlah transaksi per hari (tren)");
        MENU.put("top_products", "produk terlaris beserta omsetnya (parameter opsional limit 1-20)");
        MENU.put("sales_by_category", "komposisi penjualan per kategori dan per provider");
        MENU.put("low_stock", "produk dengan stok menipis, tidak butuh periode (parameter opsional threshold, bawaan 5)");
        MENU.put("topup_summary", "top up saldo per status dan metode (Xendit otomatis / manual)");
        MENU.put("promo_performance", "pemakaian voucher diskon dan hasil flash sale");
        MENU.put("rating_summary", "rata-rata rating toko, sebaran bintang, dan komentar pelanggan terbaru");
    }

    /** A validated menu choice. start/end are inclusive dates. */
    public record ReportRequest(String report, LocalDate start, LocalDate end, int limit, int threshold) {}

    private final TransactionHistoryRepository transactions;
    private final TopUpRepository topUps;
    private final ProductRepository products;
    private final FlashSaleRepository flashSales;
    private final RatingService ratingService;

    public Map<String, Object> run(ReportRequest r) {
        LocalDateTime from = r.start().atStartOfDay();
        LocalDateTime to = r.end().plusDays(1).atStartOfDay();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("laporan", r.report());
        if (!"low_stock".equals(r.report())) {
            out.put("periode", r.start() + " s.d. " + r.end());
        }
        switch (r.report()) {
            case "sales_summary" -> out.putAll(salesSummary(from, to));
            case "daily_sales" -> out.put("harian", dailySales(from, to));
            case "top_products" -> out.put("produk", topProducts(from, to, r.limit()));
            case "sales_by_category" -> {
                out.put("per_kategori", groupRows(transactions.salesByCategory(from, to), "kategori"));
                out.put("per_provider", groupRows(transactions.salesByProvider(from, to), "provider"));
            }
            case "low_stock" -> {
                out.put("ambang_stok", r.threshold());
                out.put("produk", lowStock(r.threshold()));
            }
            case "topup_summary" -> out.put("rincian", topupSummary(from, to));
            case "promo_performance" -> {
                out.put("voucher", voucherUsage(from, to));
                out.put("flash_sale", flashSalesIn(from, to));
            }
            case "rating_summary" -> {
                ZoneId zone = ZoneId.systemDefault();
                var f = r.start().atStartOfDay(zone).toOffsetDateTime();
                var t = r.end().plusDays(1).atStartOfDay(zone).toOffsetDateTime();
                Map<String, Object> s = ratingService.summary(f, t);
                out.put("rata_rata", s.get("average"));
                out.put("jumlah_ulasan", s.get("count"));
                out.put("sebaran_bintang", s.get("distribution"));
                out.put("komentar_terbaru", ratingService.latestComments(f, t));
            }
            default -> throw new IllegalArgumentException("Laporan tidak dikenal: " + r.report());
        }
        return out;
    }

    public Map<String, Object> salesSummary(LocalDateTime from, LocalDateTime to) {
        Object[] row = transactions.summarize(from, to).get(0);
        long revenue = money(row[1]);
        long cost = money(row[2]);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("jumlah_transaksi", ((Number) row[0]).longValue());
        m.put("omset", revenue);
        m.put("modal", cost);
        m.put("laba_kotor", revenue - cost);
        m.put("total_diskon", money(row[3]));
        m.put("pelanggan_bertransaksi", ((Number) row[4]).longValue());
        return m;
    }

    public List<Map<String, Object>> dailySales(LocalDateTime from, LocalDateTime to) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] row : transactions.dailySales(from, to)) {
            list.add(Map.of("tanggal", row[0].toString(), "transaksi", ((Number) row[1]).longValue(), "omset", money(row[2])));
        }
        return list;
    }

    public List<Map<String, Object>> monthlySales(LocalDateTime from, LocalDateTime to) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] row : transactions.monthlySales(from, to)) {
            list.add(Map.of("tahun", ((Number) row[0]).intValue(), "bulan", ((Number) row[1]).intValue(),
                    "transaksi", ((Number) row[2]).longValue(), "omset", money(row[3])));
        }
        return list;
    }

    public List<Map<String, Object>> topProducts(LocalDateTime from, LocalDateTime to, int limit) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] row : transactions.topProducts(from, to, PageRequest.of(0, limit))) {
            list.add(Map.of("nama", row[0], "terjual", ((Number) row[1]).longValue(), "omset", money(row[2])));
        }
        return list;
    }

    public List<Map<String, Object>> groupRows(List<Object[]> rows, String key) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] row : rows) {
            list.add(Map.of(key, row[0], "transaksi", ((Number) row[1]).longValue(), "omset", money(row[2])));
        }
        return list;
    }

    public List<Map<String, Object>> lowStock(int threshold) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Product p : products.findByStockLessThanEqualOrderByStockAsc(threshold)) {
            if (list.size() == 30) break;
            list.add(Map.of("nama", p.getName(), "stok", p.getStock(),
                    "provider", p.getProvider() != null ? p.getProvider().getName() : "-"));
        }
        return list;
    }

    private List<Map<String, Object>> topupSummary(LocalDateTime from, LocalDateTime to) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] row : topUps.summarize(from, to)) {
            list.add(Map.of("status", String.valueOf(row[0]), "metode", row[1],
                    "jumlah", ((Number) row[2]).longValue(), "total_nominal", money(row[3])));
        }
        return list;
    }

    private List<Map<String, Object>> voucherUsage(LocalDateTime from, LocalDateTime to) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] row : transactions.voucherUsage(from, to)) {
            list.add(Map.of("kode", row[0], "dipakai", ((Number) row[1]).longValue(),
                    "total_diskon", money(row[2]), "omset", money(row[3])));
        }
        return list;
    }

    private List<Map<String, Object>> flashSalesIn(LocalDateTime from, LocalDateTime to) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (FlashSale f : flashSales.findOverlapping(from, to)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("produk", f.getProduct().getName());
            m.put("harga_normal", f.getProduct().getPrice());
            m.put("harga_flash", f.getFlashPrice());
            m.put("terjual", f.getSoldCount() != null ? f.getSoldCount() : 0);
            m.put("kuota", f.getQuota());
            m.put("mulai", f.getStartAt().toString());
            m.put("selesai", f.getEndAt().toString());
            list.add(m);
        }
        return list;
    }

    private static long money(Object value) {
        return value == null ? 0 : Math.round(((Number) value).doubleValue());
    }
}
