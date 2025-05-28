package com.example.kartu.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.kartu.models.Kategori;
import com.example.kartu.repositories.KategoriRepository;

@Controller
public class KategoriController {
    @Autowired
    private KategoriRepository kategoriRepository;

    @GetMapping("/kategori")
    public String addKategori(Model model) {
        if (kategoriRepository.findAll().isEmpty()) {
            kategoriRepository.save(new Kategori(1, "pulsa"));
            kategoriRepository.save(new Kategori(2, "kuota"));
        }
        return "redirect:/login";
    }

}
