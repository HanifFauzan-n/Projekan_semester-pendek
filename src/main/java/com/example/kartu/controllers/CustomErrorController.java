package com.example.kartu.controllers;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String requestMethodName(@RequestParam String param) {
        return new String();
    }

    public String handleError(HttpServletRequest request, Model model) {
        // Ambil status code errornya
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        String errorMessage = "Terjadi kesalahan yang tidak diketahui.";

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());

            if (statusCode == 404) {
                errorMessage = "Halaman yang kamu cari tidak ditemukan (404).";
            } else if (statusCode == 500) {
                errorMessage = "Terjadi kesalahan pada server kami (500). Silakan coba lagi nanti.";
            } else if (statusCode == 403) {
                errorMessage = "Kamu tidak memiliki akses ke halaman ini (403).";
            } else {
                errorMessage = "Error Code: " + statusCode;
            }
        }

        model.addAttribute("errorMessage", errorMessage);
        return "error"; // Mengarah ke templates/error.html
    }
}