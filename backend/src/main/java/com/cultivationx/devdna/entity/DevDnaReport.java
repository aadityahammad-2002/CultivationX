package com.cultivationx.devdna.entity;

import com.cultivationx.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "devdna_reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class DevDnaReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Integer devDnaScore;
    private Integer interviewReadinessScore;

    // Score breakdown
    private Integer githubScore;
    private Integer leetcodeScore;
    private Integer leetgitScore;
    private Integer resumeScore;
    private Integer consistencyScore;
    private Integer aiAssessmentScore;

    @Column(columnDefinition = "TEXT")
    private String skillGraphJson;           // JSON: [{skill, score, category}]

    @Column(columnDefinition = "TEXT")
    private String strengthsJson;            // JSON: [string]

    @Column(columnDefinition = "TEXT")
    private String areasToImproveJson;       // JSON: [string]

    @Column(columnDefinition = "TEXT")
    private String insightsJson;             // JSON: [string]

    @Column(columnDefinition = "TEXT")
    private String weeklyRoadmapJson;        // JSON: {week1, week2, week3, week4}

    @Column(columnDefinition = "TEXT")
    private String interviewReadinessJson;   // Full readiness breakdown

    @Column(columnDefinition = "TEXT")
    private String todaysMissionJson;        // JSON: {title, tasks}

    @Column(columnDefinition = "TEXT")
    private String profileSummary;

    @Column(columnDefinition = "TEXT")
    private String motivationalMessage;

    @Column(nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @PrePersist
    protected void onCreate() { generatedAt = LocalDateTime.now(); }
}