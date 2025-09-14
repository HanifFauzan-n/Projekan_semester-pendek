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
            // 1. Konfigurasi Aturan Otorisasi untuk setiap request HTTP
            .authorizeHttpRequests(authorize -> authorize
                // A. URL Publik yang bisa diakses siapa saja
                .requestMatchers(
                    "/", 
                    "/login", 
                    "/daftar", 
                    "/save-akun", 
                    "/no-akun",
                    "/css/**",      // Aset statis
                    "/img/**"       // Aset statis
                ).permitAll()
                
                // B. URL khusus untuk Admin
                .requestMatchers(
                    "/home-admin/**", 
                    "/add-konter", 
                    "/update-konter/**", 
                    "/delete-konter/**"
                ).hasRole("ADMIN")
                
                // C. URL khusus untuk User
                .requestMatchers(
                    "/home-user/**", 
                    "/profile-user/**", 
                    "/pesan-konter/**", 
                    "/tambah-saldo/**", 
                    "/beli"
                ).hasRole("USER")
                
                // D. Semua URL lain harus diautentikasi (login terlebih dahulu)
                .anyRequest().authenticated()
            )
            // 2. Konfigurasi Halaman Login
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/home-redirect", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            // 3. Konfigurasi Proses Logout
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true) // Menghapus sesi setelah logout
                .deleteCookies("JSESSIONID") // Menghapus cookie sesi
                .permitAll()
            );

        return http.build();
    }
}