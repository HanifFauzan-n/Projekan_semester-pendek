package com.example.kartu.controllers;

import com.example.kartu.repositories.TopUpRepository;
import com.example.kartu.repositories.TransactionHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/transactions")
public class AdminTransactionController {

    @Autowired
    private TransactionHistoryRepository transactionHistoryRepository;

    @Autowired
    private TopUpRepository topUpRepository;

    // 1. Laporan Penjualan (Sales)
    @GetMapping("/sales")
    public String showSalesReports(Model model) {
        model.addAttribute("transactions", transactionHistoryRepository.findAllByOrderByTimestampDesc());
        return "admin_sales_report";
    }

    // 2. Laporan Top Up (Incoming Balance)
    @GetMapping("/topups")
    public String showTopUpReports(Model model) {
        model.addAttribute("topups", topUpRepository.findAllByOrderByDateDesc());
        return "admin_topup_report";
    }
}