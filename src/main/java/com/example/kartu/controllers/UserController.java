package com.example.kartu.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.kartu.models.Kategori;
import com.example.kartu.models.User;
import com.example.kartu.repositories.KategoriRepository;
import com.example.kartu.repositories.KonterRepository;
import com.example.kartu.repositories.UserRepository;

@Controller
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KonterRepository konterRepository;

    @Autowired
    private KategoriRepository kategoriRepository;

    boolean benar = true;

    @GetMapping("/")
    public String homenya(Model data) {
        data.addAttribute("ktr", konterRepository.findAll());
        data.addAttribute("kategori", kategoriRepository.findAll());
        return "home";
    }

    @GetMapping("/login")
    public String loginDulu(Model model) {

        if (kategoriRepository.findAll().isEmpty()) {
            kategoriRepository.save(new Kategori(1, "PULSA"));
            kategoriRepository.save(new Kategori(2, "KUOTA"));
        }
        User users = new User();
        if (!benar) {
            model.addAttribute("ket", "Terdapat Kesalahan, Mohon ulangi lagi");
        }
        benar = true;
        model.addAttribute("user", users);
        return "login";
    }

    @PostMapping("/cek")
    public String cek(@ModelAttribute("user") User user) {
        {
            List<User> users = userRepository.findAll();
            String admin = "Hanif18";
            String pass = "FlashCell";

            benar = true;
            if (user.getUsername().equals(admin) && user.getPassword().equals(pass)) {
                benar = true;
                return "forward:/post-login";
            }
            for (User u : users) {
                if ((user.getUsername().equals(u.getUsername()) && user.getPassword().equals(u.getPassword()))) {
                    benar = true;
                    return "forward:/post-login";
                }
            }
            benar = false;
            return "redirect:/login";
        }
    }

    @GetMapping("/no-akun")
    public String noAkun(Model data) {
        data.addAttribute("ktr", konterRepository.findAll());
        data.addAttribute("kategori", kategoriRepository.findAll());
        return "home";
    }

    @GetMapping("/add-akun")
    public String addAkun(Model model) {
        User users = new User();
        if (!benar) {
            model.addAttribute("ket", "Terdapat Kesalahan, Mohon ulangi lagi");
        }

        model.addAttribute("user", users);
        return "daftar";
    }

    @PostMapping("/save-akun")
    public String registerAkun(@ModelAttribute("user") User user) {
        List<User> users = userRepository.findAll();
        String admin = "Hanif18";
        benar = true;
        if (user.getNomer().length() < 12 || user.getDana().length() < 12
                || user.getNomer().charAt(0) != '0'
                || user.getNomer().charAt(1) != '8'
                || user.getDana().charAt(0) != '0' || user.getDana().charAt(1) != '8'
                || user.getNomer().length() > 15 || user.getDana().length() > 15 || user.getUsername().length() < 8
                || user.getPassword().length() < 8) {
            benar = false;

            return "redirect:/add-akun";
        }
        if (user.getUsername().equals(admin)) {
            benar = false;
            return "redirect:/add-akun";
        }

        for (User u : users) {
            if (user.getUsername().equals(u.getUsername())) {
                benar = false;
                return "redirect:/add-akun";
            }
        }
        benar = true;
        userRepository.save(user);
        return "redirect:/login";
    }
}
