package com.cultivationx.nexus.entity;

import com.cultivationx.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "github_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class GitHubProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String username;
    private String displayName;
    private String avatarUrl;
    private String bio;
    private String company;
    private String location;
    private String blog;
    private String email;

    // Stats
    private Integer publicRepos;
    private Integer followers;
    private Integer following;
    private Integer publicGists;
    private Integer totalStars;
    private Integer totalForks;

    // Activity
    private Integer totalCommitsThisYear;
    private Integer currentStreak;
    private Integer longestStreak;

    @Column(columnDefinition = "TEXT")
    private String topLanguagesJson;       // JSON: {"Java": 45, "Python": 30, "JS": 25}

    @Column(columnDefinition = "TEXT")
    private String repositoriesJson;       // JSON array of top repos

    @Column(columnDefinition = "TEXT")
    private String contributionDataJson;   // JSON: weekly contribution data

    private LocalDateTime githubCreatedAt;
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