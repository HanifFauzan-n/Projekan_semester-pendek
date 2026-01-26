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
                        // 1. URL Publik
                        .requestMatchers(
                                "/",
                                "/login",
                                "/register",
                                "/css/**",
                                "/img/**",
                                "/error" // Tambahkan ini biar halaman error bisa diakses siapa saja
                        ).permitAll()

                        // 2. URL Khusus Admin (UPDATE PENTING)
                        .requestMatchers(
                                "/home-admin",
                                "/profile-admin",
                                "/admin/**", // Melindungi /admin/products, /admin/providers
                                "/categories/**", // Melindungi /categories/save, /categories/delete
                                "/providers/**" // Jaga-jaga
                        ).hasRole("ADMIN")

                        // 3. URL Khusus User (UPDATE PENTING)
                        .requestMatchers(
                                "/home-user",
                                "/profile-user",
                                "/transaction/**", // Melindungi /transaction/confirm, /transaction/process
                                "/user/**", // Melindungi /user/edit, /user/update
                                "/purchase/**", // Jika masih ada sisa controller lama
                                "/topup/**" // Jika ada controller TopUp
                        ).hasRole("USER")

                        // 4. Sisanya harus login
                        .anyRequest().authenticated())
                // Config Login & Logout (Tetap sama)
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/home-redirect", true)
                        .failureUrl("/login?error=true")
                        .permitAll())
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll());

        return http.build();
    }
}