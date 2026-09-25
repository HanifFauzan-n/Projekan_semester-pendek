package com.example.kartu.config;

import com.example.kartu.services.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final com.example.kartu.services.CustomOidcUserService customOidcUserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:3000",
                "http://localhost:8080"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // CSRF: token in the readable XSRF-TOKEN cookie, sent back by the SPA as the
                // X-XSRF-TOKEN header. Only the Xendit webhook is exempt: it is a server-to-server
                // call authenticated by its X-CALLBACK-TOKEN header instead.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                        .ignoringRequestMatchers("/api/xendit/**")
                )
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                new AntPathRequestMatcher("/api/**")
                        )
                )
                .authorizeHttpRequests(authorize -> authorize
                        // 1. URL Publik: SPA Frontend, Asset Statik & REST API Publik
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/login",
                                "/register",
                                "/verify-otp",
                                "/forgot-password",
                                "/topup",
                                "/topup/**",
                                "/profile",
                                "/ulasan",
                                "/admin",
                                "/admin/**",
                                "/assets/**",
                                "/img/**",
                                "/vite.svg",
                                "/favicon.ico",
                                "/error",
                                "/api/xendit/callback",
                                "/api/xendit/webhook",
                                "/api/auth/**",
                                "/api/products/**",
                                "/api/categories/**",
                                "/api/providers/**",
                                "/api/topups/status/**",
                                "/api/flash-sales/active",
                                "/api/vouchers/active",
                                "/api/ratings/summary",
                                "/oauth2/**",
                                "/login/oauth2/**"
                        ).permitAll()

                        // Ulasan toko (SKPL-F21): daftar publik; memberi ulasan hanya pelanggan yang login
                        .requestMatchers(HttpMethod.GET, "/api/ratings").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/ratings").hasRole("USER")

                        // 2. URL Khusus Admin
                        .requestMatchers(
                                "/api/admin/**"
                        ).hasRole("ADMIN")

                        // 3. URL Khusus User (Admin juga boleh akses)
                        .requestMatchers(
                                "/api/user/**",
                                "/api/transactions/**",
                                "/api/topups/**",
                                "/api/ratings/**"
                        ).hasAnyRole("USER", "ADMIN")

                        // 4. Semua API lain butuh login (bukan terbuka secara default)
                        .requestMatchers("/api/**").authenticated()

                        // 5. Sisanya: rute SPA dan aset statis
                        .anyRequest().permitAll()
                )
                // Google OAuth2 Single Sign-On (SKPL-F03)
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                                .oidcUserService(customOidcUserService)
                        )
                        .successHandler(oAuth2LoginSuccessHandler)
                        .failureUrl(frontendUrl + "/login?error=oauth_failed")
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/api/auth/logout"))
                        .logoutSuccessHandler((req, res, auth) -> res.setStatus(HttpStatus.OK.value()))
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll());

        return http.build();
    }
}
