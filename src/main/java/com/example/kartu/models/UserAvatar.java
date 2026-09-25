package com.example.kartu.models;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Uploaded profile photo (docs/sql/011), always a re-encoded 256x256 JPEG. */
@Entity
@Data
@NoArgsConstructor
@Table(name = "user_avatars")
public class UserAvatar {

    @Id
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "image", columnDefinition = "bytea", nullable = false)
    private byte[] image;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
