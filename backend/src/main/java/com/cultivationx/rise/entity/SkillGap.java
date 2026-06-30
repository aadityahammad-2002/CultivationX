package com.cultivationx.rise.entity;

import com.cultivationx.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "skill_gaps")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class SkillGap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String existingSkillsJson;     // JSON array of {skill, proficiency, source}

    @Column(columnDefinition = "TEXT")
    private String missingSkillsJson;      // JSON array of {skill, priority, reason}

    @Column(columnDefinition = "TEXT")
    private String weeklyRoadmapJson;      // JSON array of {week, focus, tasks, resources}

    @Column(columnDefinition = "TEXT")
    private String priorityAreasJson;      // JSON array

    private Integer estimatedReadinessWeeks;
    private Integer currentReadinessPercent;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}