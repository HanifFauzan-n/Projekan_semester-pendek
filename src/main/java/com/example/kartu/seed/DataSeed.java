package com.example.kartu.seed;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.example.kartu.models.Category;
import com.example.kartu.models.User;
import com.example.kartu.repositories.CategoryRepository;
import com.example.kartu.repositories.UserRepository;

import java.util.List;

@Component
public class DataSeed implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) throws Exception {
        seedCategories();
        seedAdmin();
    }

    private void seedCategories() {
        if (categoryRepository.count() == 0) {
            System.out.println("Seeding Categories...");
            Category rowOne = new Category( "CD-001","MOBILE CREDIT");
            Category rowTwo = new Category( "CD-002","DATA PLAN");
            categoryRepository.saveAll(List.of(rowOne, rowTwo));
            System.out.println("Categories seeded.");
        }
    }

    private void seedAdmin() {
        // Cek apakah admin "Hanif18" sudah ada
        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            System.out.println("Seeding Default Admin...");
            
            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setPassword(passwordEncoder.encode(adminPassword)); // Ganti password sesuai keinginan
            admin.setRole("ROLE_ADMIN");
            admin.setPhoneNumber("081234567890");
            admin.setDanaNumber("081234567890");
            admin.setBalance(0);

            userRepository.save(admin);
            System.out.println("Admin 'Hanif18' created successfully.");
        }
    }
}