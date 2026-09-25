package com.example.kartu.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.kartu.models.UserAvatar;

public interface UserAvatarRepository extends JpaRepository<UserAvatar, Integer> {
}
