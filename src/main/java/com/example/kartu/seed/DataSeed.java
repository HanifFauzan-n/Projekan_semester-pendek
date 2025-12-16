package com.example.kartu.seed;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;

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

    @Override
    public void run(String... args) throws Exception {
        seedCategories();
        seedAdmin();
    }

    private void seedCategories() {
        if (categoryRepository.count() == 0) {
            System.out.println("Seeding Categories...");
            Category pulse = new Category( "PULSE");
            Category data = new Category( "DATA");
            categoryRepository.saveAll(List.of(pulse, data));
            System.out.println("Categories seeded.");
        }
    }

    private void seedAdmin() {
        // Cek apakah admin "Hanif18" sudah ada
        if (userRepository.findByUsername("Hanif18").isEmpty()) {
            System.out.println("Seeding Default Admin...");
            
            User admin = new User();
            admin.setUsername("Hanif18");
            admin.setPassword(passwordEncoder.encode("rahasia123")); // Ganti password sesuai keinginan
            admin.setRole("ROLE_ADMIN");
            admin.setPhoneNumber("081234567890");
            admin.setDanaNumber("081234567890");
            admin.setBalance(0);

            userRepository.save(admin);
            System.out.println("Admin 'Hanif18' created successfully.");
        }
    }
}