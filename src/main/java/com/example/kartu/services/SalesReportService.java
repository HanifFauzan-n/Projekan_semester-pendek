package com.example.kartu.services;

import com.example.kartu.models.TransactionHistory;
import com.example.kartu.repositories.TransactionHistoryRepository;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Sales report (SKPL-F16 PDF, SKPL-F17 Excel, SKPL-F19 paged table). Screen, PDF and Excel
 * read the same filter from the database, so the numbers always match.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesReportService {

    private static final Locale ID = Locale.forLanguageTag("id-ID");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", ID);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", ID);
    private static final LocalDate EARLIEST = LocalDate.of(2000, 1, 1);

    private final TransactionHistoryRepository transactions;

    /** Validated filter. start/end are inclusive; null means open ended. */
    public record Filter(LocalDate start, LocalDate end, String search) {
        public Filter {
            if (start != null && end != null && start.isAfter(end)) {
                throw new IllegalArgumentException("Tanggal awal tidak boleh setelah tanggal akhir.");
            }
        }

        LocalDateTime from() {
            return (start != null ? start : EARLIEST).atStartOfDay();
        }

        LocalDateTime to() {
            return (end != null ? end : LocalDate.now()).plusDays(1).atStartOfDay();
        }

        String pattern() {
            return search == null || search.isBlank() ? "%" : "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        }

        String label() {
            if (start == null && end == null) return "Semua periode";
            return (start != null ? start.format(DATE) : "awal") + " s.d. " + (end != null ? end.format(DATE) : "hari ini");
        }

        public String fileSuffix() {
            return (start != null ? start.toString() : "awal") + "_" + (end != null ? end.toString() : LocalDate.now().toString());
        }
    }

    public record Totals(long count, long revenue, long cost, long discount) {
        long profit() {
            return revenue - cost;
        }
    }

    public Page<TransactionHistory> page(Filter f, int page, int size) {
        return transactions.searchSales(f.from(), f.to(), f.pattern(),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
    }

    public Totals totals(Filter f) {
        Object[] r = transactions.summarizeSales(f.from(), f.to(), f.pattern()).get(0);
        return new Totals(((Number) r[0]).longValue(), money(r[1]), money(r[2]), money(r[3]));
    }

    public static long cost(TransactionHistory t) {
        double paid = t.getAmountPaid() != null ? t.getAmountPaid() : 0;
        return Math.round(t.getCostPrice() != null ? t.getCostPrice() : paid * 0.9);
    }

    // ---------------------------------------------------------------- PDF (SKPL-F16)

    public byte[] pdf(Filter f) {
        List<TransactionHistory> rows = transactions.searchSalesForExport(f.from(), f.to(), f.pattern());
        Totals totals = totals(f);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 30, 30);
        PdfWriter.getInstance(doc, out);
        doc.open();

        Font kopTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Font small = FontFactory.getFont(FontFactory.HELVETICA, 9);
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        Font cell = FontFactory.getFont(FontFactory.HELVETICA, 8);
        Font head = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);

        // Kop toko
        doc.add(centered("ZELATAN CELL", kopTitle));
        doc.add(centered("Jl. Temugiri 01 Tinggarjati Lor, Desa Gentasari, Kec. Kroya, Kab. Cilacap, Jawa Tengah 53282", small));
        doc.add(centered("Konter Pulsa, Paket Data, Token Listrik, dan Produk Telekomunikasi", small));
        doc.add(new Chunk(new LineSeparator(1.5f, 100, Color.BLACK, Element.ALIGN_CENTER, -4)));
        doc.add(Chunk.NEWLINE);
        doc.add(centered("LAPORAN PENJUALAN", bold));
        doc.add(centered("Periode: " + f.label()
                + (f.search() != null && !f.search().isBlank() ? "  |  Pencarian: \"" + f.search().trim() + "\"" : ""), small));
        doc.add(centered("Dicetak: " + LocalDateTime.now().format(DATE_TIME), small));
        doc.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(new float[]{3, 9, 11, 9, 16, 7, 8, 9, 9, 9});
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        for (String h : new String[]{"No", "Tanggal", "ID Transaksi", "Pelanggan", "Produk", "Voucher", "Diskon",
                "Total Bayar", "Modal", "Laba"}) {
            PdfPCell c = new PdfPCell(new Phrase(h, head));
            c.setBackgroundColor(new Color(37, 99, 235));
            c.setPadding(5);
            table.addCell(c);
        }
        int no = 1;
        for (TransactionHistory t : rows) {
            long paid = money(t.getAmountPaid());
            long cost = cost(t);
            table.addCell(pdfCell(String.valueOf(no++), cell, Element.ALIGN_CENTER));
            table.addCell(pdfCell(t.getTimestamp() != null ? t.getTimestamp().format(DATE_TIME) : "-", cell, Element.ALIGN_LEFT));
            table.addCell(pdfCell(t.getTransactionId(), cell, Element.ALIGN_LEFT));
            table.addCell(pdfCell(t.getCustomer(), cell, Element.ALIGN_LEFT));
            table.addCell(pdfCell(t.getProduct() != null ? t.getProduct().getName() : "-", cell, Element.ALIGN_LEFT));
            table.addCell(pdfCell(t.getVoucherCode() != null ? t.getVoucherCode() : "-", cell, Element.ALIGN_CENTER));
            table.addCell(pdfCell(rupiah(money(t.getDiscountAmount())), cell, Element.ALIGN_RIGHT));
            table.addCell(pdfCell(rupiah(paid), cell, Element.ALIGN_RIGHT));
            table.addCell(pdfCell(rupiah(cost), cell, Element.ALIGN_RIGHT));
            table.addCell(pdfCell(rupiah(paid - cost), cell, Element.ALIGN_RIGHT));
        }
        if (rows.isEmpty()) {
            PdfPCell empty = pdfCell("Tidak ada transaksi pada periode ini.", cell, Element.ALIGN_CENTER);
            empty.setColspan(10);
            empty.setPadding(12);
            table.addCell(empty);
        }
        doc.add(table);
        doc.add(Chunk.NEWLINE);

        PdfPTable summary = new PdfPTable(new float[]{3, 2});
        summary.setWidthPercentage(40);
        summary.setHorizontalAlignment(Element.ALIGN_RIGHT);
        String[][] lines = {
                {"Jumlah transaksi", String.valueOf(totals.count())},
                {"Total omset", rupiah(totals.revenue())},
                {"Total modal", rupiah(totals.cost())},
                {"Laba kotor", rupiah(totals.profit())},
                {"Total diskon diberikan", rupiah(totals.discount())},
        };
        for (String[] line : lines) {
            summary.addCell(pdfCell(line[0], bold, Element.ALIGN_LEFT));
            summary.addCell(pdfCell(line[1], bold, Element.ALIGN_RIGHT));
        }
        doc.add(summary);
        doc.close();
        return out.toByteArray();
    }

    private static Paragraph centered(String text, Font font) {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_CENTER);
        return p;
    }

    private static PdfPCell pdfCell(String text, Font font, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text == null ? "-" : text, font));
        c.setHorizontalAlignment(align);
        c.setPadding(4);
        return c;
    }

    // ---------------------------------------------------------------- Excel (SKPL-F17)

    public byte[] excel(Filter f) {
        List<TransactionHistory> rows = transactions.searchSalesForExport(f.from(), f.to(), f.pattern());
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Laporan Penjualan");
            DataFormat format = wb.createDataFormat();

            org.apache.poi.ss.usermodel.Font boldFont = wb.createFont();
            boldFont.setBold(true);
            org.apache.poi.ss.usermodel.Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            org.apache.poi.ss.usermodel.Font whiteBold = wb.createFont();
            whiteBold.setBold(true);
            whiteBold.setColor(IndexedColors.WHITE.getIndex());

            CellStyle titleStyle = wb.createCellStyle();
            titleStyle.setFont(titleFont);
            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFont(whiteBold);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            CellStyle money = wb.createCellStyle();
            money.setDataFormat(format.getFormat("\"Rp\" #,##0"));
            CellStyle moneyBold = wb.createCellStyle();
            moneyBold.setDataFormat(format.getFormat("\"Rp\" #,##0"));
            moneyBold.setFont(boldFont);
            CellStyle dateStyle = wb.createCellStyle();
            dateStyle.setDataFormat(format.getFormat("dd/mm/yyyy hh:mm"));
            CellStyle boldStyle = wb.createCellStyle();
            boldStyle.setFont(boldFont);

            sheet.createRow(0).createCell(0).setCellValue("ZELATAN CELL - LAPORAN PENJUALAN");
            sheet.getRow(0).getCell(0).setCellStyle(titleStyle);
            sheet.createRow(1).createCell(0).setCellValue("Periode: " + f.label());
            sheet.createRow(2).createCell(0).setCellValue("Dicetak: " + LocalDateTime.now().format(DATE_TIME));

            String[] headers = {"No", "Tanggal", "ID Transaksi", "Pelanggan", "Produk", "Voucher", "Diskon",
                    "Total Bayar", "Modal", "Laba"};
            int headerRow = 4;
            Row h = sheet.createRow(headerRow);
            for (int i = 0; i < headers.length; i++) {
                Cell c = h.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int r = headerRow + 1;
            for (TransactionHistory t : rows) {
                Row row = sheet.createRow(r);
                row.createCell(0).setCellValue(r - headerRow);
                Cell date = row.createCell(1);
                if (t.getTimestamp() != null) {
                    date.setCellValue(t.getTimestamp());
                }
                date.setCellStyle(dateStyle);
                row.createCell(2).setCellValue(t.getTransactionId());
                row.createCell(3).setCellValue(t.getCustomer() != null ? t.getCustomer() : "-");
                row.createCell(4).setCellValue(t.getProduct() != null ? t.getProduct().getName() : "-");
                row.createCell(5).setCellValue(t.getVoucherCode() != null ? t.getVoucherCode() : "-");
                numeric(row, 6, money(t.getDiscountAmount()), money);
                numeric(row, 7, money(t.getAmountPaid()), money);
                numeric(row, 8, cost(t), money);
                Cell profit = row.createCell(9);
                profit.setCellFormula("H" + (r + 1) + "-I" + (r + 1)); // live formula, not a Java number
                profit.setCellStyle(money);
                r++;
            }
            if (rows.isEmpty()) {
                sheet.createRow(r).createCell(0).setCellValue("Tidak ada transaksi pada periode ini.");
                sheet.addMergedRegion(new CellRangeAddress(r, r, 0, 9));
                r++;
            }

            // Totals as SUM formulas so the sheet stays a live spreadsheet.
            Row total = sheet.createRow(r);
            Cell label = total.createCell(0);
            label.setCellValue("TOTAL (" + rows.size() + " transaksi)");
            label.setCellStyle(boldStyle);
            sheet.addMergedRegion(new CellRangeAddress(r, r, 0, 5));
            int first = headerRow + 2; // Excel row numbers are 1-based
            int last = Math.max(first, r);
            for (int col = 6; col <= 9; col++) {
                String letter = String.valueOf((char) ('A' + col));
                Cell c = total.createCell(col);
                c.setCellFormula(rows.isEmpty() ? "0" : "SUM(" + letter + first + ":" + letter + last + ")");
                c.setCellStyle(moneyBold);
            }

            sheet.createFreezePane(0, headerRow + 1);
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Gagal membuat file Excel", e);
        }
    }

    private static void numeric(Row row, int col, long value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value);
        c.setCellStyle(style);
    }

    private static long money(Object value) {
        return value == null ? 0 : Math.round(((Number) value).doubleValue());
    }

    private static String rupiah(long value) {
        return "Rp " + NumberFormat.getNumberInstance(ID).format(value);
    }
}
