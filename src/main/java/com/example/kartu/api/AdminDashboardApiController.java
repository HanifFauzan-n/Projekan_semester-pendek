package com.example.kartu.api;

import com.example.kartu.enums.PurchaseTarget;
import com.example.kartu.models.TransactionHistory;
import com.example.kartu.repositories.ProductRepository;
import com.example.kartu.repositories.TransactionHistoryRepository;
import com.example.kartu.repositories.UserRepository;
import com.example.kartu.services.RatingService;
import com.example.kartu.services.ReportService;
import com.example.kartu.services.SalesReportService;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Transactional(readOnly = true) // one round trip for BEGIN/COMMIT per request, not per query
public class AdminDashboardApiController {

    private final TransactionHistoryRepository transactionHistoryRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ReportService reportService;
    private final RatingService ratingService;
    private final SalesReportService salesReportService;

    // SKPL-F15: all totals are aggregated by the database through ReportService (JPQL),
    // the same queries the AI chat uses, instead of loading every transaction into memory.
    private static final LocalDateTime ALL_TIME = LocalDate.of(2000, 1, 1).atStartOfDay();
    private static final int LOW_STOCK_THRESHOLD = 5;
    private static final Locale ID = Locale.forLanguageTag("id-ID");

    @GetMapping("/dashboard/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        LocalDateTime tomorrow = LocalDate.now().plusDays(1).atStartOfDay();
        Map<String, Object> sales = reportService.salesSummary(ALL_TIME, tomorrow);
        Map<String, Object> rating = ratingService.summaryAllTime();

        Map<String, Object> resp = new HashMap<>();
        resp.put("totalOmset", sales.get("omset"));
        resp.put("totalProfit", sales.get("laba_kotor"));
        resp.put("totalTransactions", sales.get("jumlah_transaksi"));
        resp.put("activeCustomers", sales.get("pelanggan_bertransaksi"));
        resp.put("lowStockCount", productRepository.countByStockLessThanEqual(LOW_STOCK_THRESHOLD));
        resp.put("totalProducts", productRepository.count());
        resp.put("totalUsers", userRepository.count());
        resp.put("ratingAverage", rating.get("average"));
        resp.put("ratingCount", rating.get("count"));
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/dashboard/charts")
    public ResponseEntity<Map<String, Object>> getCharts() {
        LocalDate today = LocalDate.now();
        LocalDateTime tomorrow = today.plusDays(1).atStartOfDay();

        // 1. Daily trend, last 30 days; days without sales are shown as 0.
        Map<LocalDate, Long> perDay = new HashMap<>();
        for (Map<String, Object> d : reportService.dailySales(today.minusDays(29).atStartOfDay(), tomorrow)) {
            perDay.put(LocalDate.parse((String) d.get("tanggal")), (Long) d.get("omset"));
        }
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("dd MMM", ID);
        List<String> dailyLabels = new ArrayList<>();
        List<Long> dailyValues = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            dailyLabels.add(d.format(dayFmt));
            dailyValues.add(perDay.getOrDefault(d, 0L));
        }

        // 2. Monthly sales, last 12 months including the current one.
        YearMonth firstMonth = YearMonth.from(today).minusMonths(11);
        Map<YearMonth, Long> perMonth = new HashMap<>();
        for (Map<String, Object> m : reportService.monthlySales(firstMonth.atDay(1).atStartOfDay(), tomorrow)) {
            perMonth.put(YearMonth.of((Integer) m.get("tahun"), (Integer) m.get("bulan")), (Long) m.get("omset"));
        }
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MMM yyyy", ID);
        List<String> monthlyLabels = new ArrayList<>();
        List<Long> monthlyValues = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            YearMonth ym = firstMonth.plusMonths(i);
            monthlyLabels.add(ym.format(monthFmt));
            monthlyValues.add(perMonth.getOrDefault(ym, 0L));
        }

        // 3. Top 5 products and 4. category composition, all time.
        List<Map<String, Object>> topProducts = reportService.topProducts(ALL_TIME, tomorrow, 5).stream()
                .map(p -> Map.<String, Object>of("name", p.get("nama"), "salesCount", p.get("terjual")))
                .collect(Collectors.toList());
        List<String> categoryLabels = new ArrayList<>();
        List<Long> categoryValues = new ArrayList<>();
        for (Object[] row : transactionHistoryRepository.salesByCategory(ALL_TIME, tomorrow)) {
            categoryLabels.add(String.valueOf(row[0]));
            categoryValues.add(((Number) row[1]).longValue());
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("dailyLabels", dailyLabels);
        resp.put("dailyValues", dailyValues);
        resp.put("monthlyLabels", monthlyLabels);
        resp.put("monthlyValues", monthlyValues);
        resp.put("topProducts", topProducts);
        resp.put("categoryLabels", categoryLabels);
        resp.put("categoryValues", categoryValues);
        return ResponseEntity.ok(resp);
    }

    // SKPL-F19: filtered and paged by the database; SKPL-F16/F17: same filter exported.
    @GetMapping("/reports/sales")
    public ResponseEntity<?> getSalesReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        SalesReportService.Filter filter;
        try {
            filter = filter(startDate, endDate, search);
        } catch (IllegalArgumentException | DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Rentang tanggal tidak valid."));
        }

        Page<TransactionHistory> result = salesReportService.page(filter, page, size);
        List<Map<String, Object>> items = result.getContent().stream().map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("transactionId", t.getTransactionId());
            m.put("serialNumber", t.getSerialNumber());
            m.put("customer", t.getCustomer());
            m.put("customerNumber", t.getCustomerNumber());
            m.put("target", PurchaseTarget.of(t.getProduct()));
            m.put("productName", t.getProduct() != null ? t.getProduct().getName() : "-");
            m.put("amountPaid", t.getAmountPaid());
            m.put("costPrice", SalesReportService.cost(t));
            m.put("discountAmount", t.getDiscountAmount());
            m.put("voucherCode", t.getVoucherCode());
            m.put("status", t.getStatus());
            m.put("timestamp", t.getTimestamp());
            return m;
        }).collect(Collectors.toList());

        SalesReportService.Totals totals = salesReportService.totals(filter);
        Map<String, Object> resp = new HashMap<>();
        resp.put("items", items);
        resp.put("page", result.getNumber());
        resp.put("size", result.getSize());
        resp.put("totalPages", result.getTotalPages());
        resp.put("totalElements", result.getTotalElements());
        resp.put("totalRevenue", totals.revenue());
        resp.put("totalCost", totals.cost());
        resp.put("totalProfit", totals.revenue() - totals.cost());
        resp.put("totalDiscount", totals.discount());
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/reports/sales/pdf")
    public ResponseEntity<?> salesPdf(@RequestParam(required = false) String startDate,
                                      @RequestParam(required = false) String endDate,
                                      @RequestParam(required = false) String search) {
        return download(startDate, endDate, search, "pdf", MediaType.APPLICATION_PDF);
    }

    @GetMapping("/reports/sales/excel")
    public ResponseEntity<?> salesExcel(@RequestParam(required = false) String startDate,
                                        @RequestParam(required = false) String endDate,
                                        @RequestParam(required = false) String search) {
        return download(startDate, endDate, search, "xlsx",
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    private ResponseEntity<?> download(String startDate, String endDate, String search, String ext, MediaType type) {
        SalesReportService.Filter filter;
        try {
            filter = filter(startDate, endDate, search);
        } catch (IllegalArgumentException | DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Rentang tanggal tidak valid."));
        }
        byte[] file = "pdf".equals(ext) ? salesReportService.pdf(filter) : salesReportService.excel(filter);
        String name = "laporan-penjualan-" + filter.fileSuffix() + "." + ext;
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(name).build().toString())
                .body(file);
    }

    private static SalesReportService.Filter filter(String startDate, String endDate, String search) {
        LocalDate start = startDate != null && !startDate.isBlank() ? LocalDate.parse(startDate) : null;
        LocalDate end = endDate != null && !endDate.isBlank() ? LocalDate.parse(endDate) : null;
        return new SalesReportService.Filter(start, end, search);
    }
}
