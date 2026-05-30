package com.ghostcoach.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    /** Per-sport profile: each entry holds the sport, the player's role in that sport, and their level. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_sport_profiles", joinColumns = @JoinColumn(name = "user_id"))
    @Builder.Default
    private List<SportProfile> sportProfiles = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
