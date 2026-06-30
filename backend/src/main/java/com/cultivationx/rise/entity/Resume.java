package com.cultivationx.rise.entity;

import com.cultivationx.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "resumes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileType;   // application/pdf or docx

    private Long fileSize;

    @Column(nullable = false)
    private String filePath;

    @Column(columnDefinition = "TEXT")
    private String rawText;

    // ===== ATS Analysis Results =====
    private Integer atsScore;
    private Integer keywordMatchScore;
    private Integer formattingScore;
    private Integer actionVerbScore;
    private Integer quantifiedAchievementsScore;

    @Column(columnDefinition = "TEXT")
    private String extractedSkillsJson;    // JSON array

    @Column(columnDefinition = "TEXT")
    private String missingKeywordsJson;    // JSON array

    @Column(columnDefinition = "TEXT")
    private String improvementsJson;       // JSON array

    @Column(columnDefinition = "TEXT")
    private String strengthsJson;          // JSON array

    @Column(columnDefinition = "TEXT")
    private String experienceSummary;

    @Column(columnDefinition = "TEXT")
    private String educationSummary;

    @Column(columnDefinition = "TEXT")
    private String projectsSummary;

    @Column(columnDefinition = "TEXT")
    private String overallFeedback;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ResumeStatus status = ResumeStatus.UPLOADED;

    @Builder.Default
    private boolean active = true;

    private Integer version;

    private LocalDateTime analyzedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }

    public enum ResumeStatus {
        UPLOADED, PARSING, ANALYZING, ANALYZED, FAILED
    }
}