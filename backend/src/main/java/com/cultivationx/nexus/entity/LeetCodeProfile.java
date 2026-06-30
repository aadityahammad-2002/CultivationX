package com.cultivationx.nexus.entity;

import com.cultivationx.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "leetcode_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class LeetCodeProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String username;
    private String realName;
    private String avatarUrl;
    private String ranking;

    // Problem Stats
    private Integer totalSolved;
    private Integer easySolved;
    private Integer mediumSolved;
    private Integer hardSolved;
    private Integer totalSubmissions;
    private Double acceptanceRate;

    // Contest
    private Double contestRating;
    private Integer contestRanking;
    private Integer contestsAttended;

    // Streak
    private Integer currentStreak;
    private Integer longestStreak;
    private Integer totalActiveDays;

    @Column(columnDefinition = "TEXT")
    private String recentSubmissionsJson;  // JSON array

    @Column(columnDefinition = "TEXT")
    private String languageStatsJson;      // JSON: {Java: 150, Python: 50}

    @Column(columnDefinition = "TEXT")
    private String badgesJson;             // JSON array

    @Column(columnDefinition = "TEXT")
    private String submissionCalendarJson; // JSON heatmap data

    private LocalDateTime lastSyncAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}