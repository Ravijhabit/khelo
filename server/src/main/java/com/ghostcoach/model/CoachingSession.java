package com.ghostcoach.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.ghostcoach.model.SportType;

@Entity
@Table(name = "coaching_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoachingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String imagePath;

    private Integer overallScore;

    @Column(columnDefinition = "TEXT")
    private String strengths;

    @Column(columnDefinition = "TEXT")
    private String areasToImprove;

    @Column(columnDefinition = "TEXT")
    private String priorityFix;

    @Column(columnDefinition = "TEXT")
    private String drillSuggestion;

    private String confidenceLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private SportType sport;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
