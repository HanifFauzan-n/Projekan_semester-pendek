package com.example.kartu.controllers;

import com.example.kartu.services.TopUpService;
import com.example.kartu.services.TransactionHistoryService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/transactions")
public class AdminTransactionController {

    
    private final TransactionHistoryService transactionService; // Pakai layanan

    
    private final TopUpService topUpService; // Pakai layanan

    @GetMapping("/sales")
    public String showSalesReports(Model model) {
        // Panggil metode baru di layanan
        model.addAttribute("transactions", transactionService.getAllTransactionsDesc());
        return "admin_sales_report";
    }

    @GetMapping("/topups")
    public String showTopUpReports(Model model) {
        // Panggil metode baru di layanan
        model.addAttribute("topups", topUpService.getAllTopUpsDesc());
        return "admin_topup_report";
    }
}