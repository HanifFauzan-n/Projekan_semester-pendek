package com.example.kartu.services;

import com.example.kartu.models.User;
import com.example.kartu.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Cari pengguna berdasarkan username di database
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User tidak ditemukan dengan username: " + username));

        // Jika statusnya BANNED, maka isAccountNonLocked menjadi false
        boolean isAccountNonLocked = !"BANNED".equals(user.getStatus());

        // Gunakan konstruktor UserDetails yang lengkap (7 parameter)
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                true,                // enabled
                true,                // accountNonExpired
                true,                // credentialsNonExpired
                isAccountNonLocked,  // accountNonLocked (false jika user BANNED)
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole()))
        );
    }
}