package com.example.kartu.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                // 1. URL Publik (dapat diakses tanpa login)
                .requestMatchers(
                    "/",
                    "/login",
                    "/register", // URL untuk GET (menampilkan form) dan POST (memproses registrasi)
                    "/css/**",   // Aset statis
                    "/img/**"    // Aset statis
                ).permitAll()

                // 2. URL Khusus Admin
                .requestMatchers(
                    "/home-admin",
                    "/profile-admin",
                    "/add-product",       // Sebelumnya /add-konter
                    "/save-product",      // URL baru untuk menyimpan produk
                    "/update-product/**", // Sebelumnya /update-konter/**
                    "/delete-product/**"  // Sebelumnya /delete-konter/**
                ).hasRole("ADMIN")

                // 3. URL Khusus User
                .requestMatchers(
                    "/home-user",
                    "/profile-user",
                    "/purchase-product/**", // Sebelumnya /pesan-konter/**
                    "/add-balance/**",      // Sebelumnya /tambah-saldo/**
                    "/purchase"             // Sebelumnya /beli
                ).hasRole("USER")

                // 4. Semua URL lainnya harus diautentikasi
                .anyRequest().authenticated()
            )
            // Konfigurasi Halaman Login
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/home-redirect", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            // Konfigurasi Proses Logout
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }
}