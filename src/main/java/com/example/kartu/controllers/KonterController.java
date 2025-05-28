package com.example.kartu.controllers;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.kartu.models.Histori;
import com.example.kartu.models.Konter;
import com.example.kartu.models.User;
import com.example.kartu.repositories.HistoriRepository;
import com.example.kartu.repositories.KategoriRepository;
import com.example.kartu.repositories.KonterRepository;
import com.example.kartu.repositories.UserRepository;

@Controller
public class KonterController {
    @Autowired
    private KonterRepository konterRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KategoriRepository kategoriRepository;

    @Autowired
    private HistoriRepository historiRepository;

    public boolean benar = true;

    int lagilogin;
    public boolean delet = true;
    public Integer yanglogin = 0;

    @GetMapping("/add-konter")
    public String addKonter(Model data) {
        if (yanglogin == 10) {
            Konter konter = new Konter();
            data.addAttribute("ktr", konter);
            data.addAttribute("kategori", kategoriRepository.findAll());
            return "addKonter";
        }
        return "redirect:/login";
    }

    @PostMapping("/save-konter")
    public String savekonter(@ModelAttribute("ktr") Konter konter) {
        List<Konter> konters = konterRepository.findAll();
        if (yanglogin == 10) {
            for (Konter k : konters) {
                if (konter.getHarga() < 0 || konter.getStok() < 0 || konter.getId() < 0
                        || konter.getKuota().charAt(0) == '-') {
                    return "redirect:/home-admin";
                }
                if (konter.getId().equals(k.getId())) {
                    konter.setGambar("/img/" + konter.getNama() + ".png");
                    konterRepository.save(konter);
                    return "redirect:/home-admin";
                }

            }
            konter.setGambar("/img/" + konter.getNama() + ".png");
            konterRepository.save(konter);
            return "redirect:/home-admin";
        }
        return "redirect:/login";
    }

    @PostMapping("/post-login")
    public String loginAkun(@ModelAttribute("user") User user) {
        List<User> users = userRepository.findAll();
        String admin = "Hanif18";
        String pass = "FlashCell";
        benar = true;
        if (user.getUsername().equals(admin) && user.getPassword().equals(pass)) {
            benar = true;
            yanglogin = 10;
            return "redirect:/home-admin";

        }
        for (User u : users) {
            if (user.getUsername().equals(u.getUsername()) && user.getPassword().equals(u.getPassword())) {
                benar = true;
                lagilogin = u.getId();
                yanglogin = 5;
                return "redirect:/home-user";
            }

        }
        // userRepository.save(user);
        benar = false;
        return "redirect:/login";
    }

    @GetMapping("/home-admin")
    public String konterAdmin(Model data) {
        if (yanglogin == 10) {
            if(delet == false){
                data.addAttribute("ket","Tidak dapat dihapus karena terhubung dengan Tabel Histori" );
                delet = true;
            }
            if (konterRepository.findAll().isEmpty()) {
                data.addAttribute("add", "Barang Belum Ada");
                return "home_admin";
            } else {
                data.addAttribute("add", "Produk Yang Tersedia");
                data.addAttribute("ktr", konterRepository.findAll());
                data.addAttribute("kategori", kategoriRepository.findAll());
                return "home_admin";
            }
        }
        return "redirect:/login";
    }

    @GetMapping("/home-user")
    public String konterUser(Model data) {
        if (yanglogin == 5) {
            if (konterRepository.findAll().isEmpty()) {
                data.addAttribute("add", "Barang Belum Ada");
                return "home_user";
            } else {
                data.addAttribute("ktr", konterRepository.findAll());
                data.addAttribute("add", "Produk Yang Tersedia");

                return "home_user";
            }
        }
        return "redirect:/login";
    }

    @GetMapping("/update-konter/{id}")
    public String updateKonter(@PathVariable(value = "id") Integer id, Model data) {
        if (yanglogin == 10) {
            Konter konter = konterRepository.getReferenceById(id);
            data.addAttribute("ktr", konter);
            return "updateKonter";
        }
        return "redirect:/login";
    }

    @GetMapping("/tambah-saldo/{id}")
    public String saldo(@PathVariable(value = "id") Integer id, Model data) {
        if (yanglogin == 5) {
            User user = userRepository.getReferenceById(id);
            data.addAttribute("user", user);
            return "tambah_saldo";
        }
        return "redirect:/login";
    }

    @GetMapping("/pesan-konter/{id}")
    public String pesanKuota(@PathVariable(value = "id") Integer id, Model data) {
        if (yanglogin == 5) {
            Konter konter = konterRepository.getReferenceById(id);
            if (konter.getStok() == 0) {
                return "redirect:/";
            }
            data.addAttribute("ktr", konter);
            return "pesanKuota";
        }
        return "redirect:/login";
    }

    @GetMapping("/delete-konter/{id}")
    public String deletekonter(@PathVariable(value = "id") Integer id) {
        Konter konter = konterRepository.getReferenceById(id);
        List<Histori> historis = historiRepository.findAll();
        for (Histori h : historis) {
            if (h.getKonter().getId() == konter.getId()) {
                delet = false;
                return "redirect:/home-admin";
            }
        }

        if (yanglogin == 10) {
            konterRepository.deleteById(id);
            return "redirect:/home-admin";
        }
        return "redirect:/login";
    }

    @GetMapping("/search")
    public String adminSearch(@RequestParam(name = "search") String nama, Model model) {
        List<Konter> konters = konterRepository.findByNamaContainingIgnoreCase(nama);
        model.addAttribute("ktr", konters);
        if (yanglogin == 10) {
            return "home_admin";
        }
        if (yanglogin == 5) {
            return "home_user";
        }
        return "home";
    }

    @GetMapping("/kategori-pulsa")
    public String pulsa(Model model) {
        List<Konter> konters = konterRepository.findByKodeKategoriTipe("pulsa");
        model.addAttribute("ktr", konters);
        if (yanglogin == 10) {
            return "home_admin";
        }
        if (yanglogin == 5) {
            return "home_user";
        }
        return "home";
    }

    @GetMapping("/kategori-kuota")
    public String kuota(Model model) {
        List<Konter> konters = konterRepository.findByKodeKategoriTipe("kuota");
        model.addAttribute("ktr", konters);
        if (yanglogin == 10) {
            return "home_admin";
        }
        if (yanglogin == 5) {
            return "home_user";
        }
        return "home";
    }

    @PostMapping("/tambah-saldo")
    public String saveSaldo(@ModelAttribute("user") User user) {
        User usr = userRepository.getReferenceById(lagilogin);
        if (yanglogin == 5) {
            if (user.getSaldo() < 0) {
                return "redirect:/profile-user";
            }
            user.setSaldo(user.getSaldo() + usr.getSaldo());
            userRepository.save(user);
            return "redirect:/profile-user";
        }
        return "redirect:/login";
    }

    @PostMapping("/beli")
    public String terbeli(@ModelAttribute("ktr") Konter konter) {
        if (yanglogin == 5) {
            User user = userRepository.getReferenceById(lagilogin);
            Histori histori = new Histori();
            LocalDateTime tanggalSekarang = LocalDateTime.now();
            if (konter.getHarga() > user.getSaldo()) {
                return "redirect:/profile-user";

            } else {
                konter.setStok(konter.getStok() - 1);
                user.setSaldo(user.getSaldo() - konter.getHarga());
                konterRepository.save(konter);
                histori.setUser(user);
                histori.setKonter(konter);
                histori.setTanggal(tanggalSekarang);
                historiRepository.save(histori);
                return "redirect:/home-user";
            }
        }
        return "redirect:/login";

    }

    @GetMapping("/profile-user")
    public String profile(Model model) {
        if (yanglogin == 5) {
            User user = userRepository.getReferenceById(lagilogin);
            model.addAttribute("user", user);
            List<Histori> historis = historiRepository.findByUserId(lagilogin);
            int jumlah = historis.size();
            int banyak = 0;
            for (Histori h : historis) {
                banyak += h.getKonter().getHarga();
            }

            model.addAttribute("histori", historis);
            model.addAttribute("jumlah", jumlah);
            model.addAttribute("banyak", banyak);
            return "profile_user";
        }
        return "redirect:/login";
    }

    @GetMapping("profile-admin")
    public String profiles(Model model) {
        if (yanglogin == 10) {
            List<Histori> historis = historiRepository.findAll();
            int banyak = 0;
            for (Histori h : historis) {
                banyak += h.getKonter().getHarga();
            }
            model.addAttribute("banyak", banyak);
            List<Konter> konters = konterRepository.findAll();
            int jumlah = 0;
            for (Konter k : konters) {
                k.getNama();
                jumlah++;
            }
            model.addAttribute("jumlah", jumlah);

            return "profile_admin";
        }
        return "redirect:/login";
    }

}
