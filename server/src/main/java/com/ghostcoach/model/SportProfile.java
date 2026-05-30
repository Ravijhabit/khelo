package com.ghostcoach.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SportProfile {

    @Column(name = "sport", nullable = false)
    private String sport;

    @Column(name = "position", nullable = false)
    private String position;

    @Column(name = "experience_level", nullable = false)
    private String experienceLevel;
}
