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

       String errorMessage = "An unexpected error occurred. Please try again later."; // gw nggak tau bagusnya gini atau nggak intinya cuman buat nenangin aja dari pada keluar error yang panjang terus user nya nggak paham

    if (status != null) {
        int statusCode = Integer.parseInt(status.toString());

        if (statusCode == 404) {
            errorMessage = "The page you are looking for could not be found (404).";
        } else if (statusCode == 500) {
            errorMessage = "Internal server error (500). Please try зgain later.";
        } else if (statusCode == 403) {
            errorMessage = "You do not have permission to access this page (403).";
        } else {
            errorMessage = "Error Code: " + statusCode;
        }
    }

        model.addAttribute("errorMessage", errorMessage);
        return "error"; // Mengarah ke templates/error.html
    }
}