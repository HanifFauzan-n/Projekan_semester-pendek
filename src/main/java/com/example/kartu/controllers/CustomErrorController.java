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

       String errorMessage = "An unexpected error occurred. Please try again later."; // Pesan umum agar detail kesalahan internal tidak ditampilkan kepada pengguna

    if (status != null) {
        int statusCode = Integer.parseInt(status.toString());

        if (statusCode == 404) {
            errorMessage = "The page you are looking for was not found (404).";
        } else if (statusCode == 500) {
            errorMessage = "A server error occurred (500). Please try again later.";
        } else if (statusCode == 403) {
            errorMessage = "You do not have permission to access this page (403).";
        } else {
            errorMessage = "Error code: " + statusCode;
        }
    }

        model.addAttribute("errorMessage", errorMessage);
        return "error"; // Mengarah ke templates/error.html
    }
}
