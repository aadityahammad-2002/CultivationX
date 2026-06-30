package com.cultivationx.nexus.entity;

import com.cultivationx.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "leetgit_syncs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class LeetGitSync {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String problemTitle;
    private String problemSlug;
    private String difficulty;
    private String language;

    @Column(columnDefinition = "TEXT")
    private String userCode;

    @Column(columnDefinition = "TEXT")
    private String aiReviewJson;

    @Column(columnDefinition = "TEXT")
    private String optimizedCode;

    @Column(columnDefinition = "TEXT")
    private String readmeContent;

    @Column(columnDefinition = "TEXT")
    private String interviewNotes;

    private String timeComplexity;
    private String spaceComplexity;
    private Integer aiScore;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SyncStatus status = SyncStatus.PENDING;

    private String githubCommitUrl;
    private String githubFilePath;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime syncedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public enum SyncStatus {
        PENDING, AI_REVIEWING, PUSHING, SYNCED, FAILED
    }
}