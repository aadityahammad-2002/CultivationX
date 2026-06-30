package com.cultivationx.nexus.dto;

import com.cultivationx.nexus.entity.LeetGitSync;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class NexusDto {

    // ===== GITHUB =====
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GitHubProfileResponse {
        private String username;
        private String displayName;
        private String avatarUrl;
        private String bio;
        private String company;
        private String location;
        private Integer publicRepos;
        private Integer followers;
        private Integer following;
        private Integer totalStars;
        private Integer totalForks;
        private Integer totalCommitsThisYear;
        private Integer currentStreak;
        private Integer longestStreak;
        private Map<String, Integer> topLanguages;
        private List<Map<String, Object>> repositories;
        private LocalDateTime lastSyncAt;
        private boolean connected;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GitHubConnectRequest {
        @NotBlank(message = "GitHub OAuth code is required")
        private String code;
    }

    // ===== LEETCODE =====
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeetCodeProfileResponse {
        private String username;
        private String realName;
        private String avatarUrl;
        private String ranking;
        private Integer totalSolved;
        private Integer easySolved;
        private Integer mediumSolved;
        private Integer hardSolved;
        private Double acceptanceRate;
        private Double contestRating;
        private Integer currentStreak;
        private Integer longestStreak;
        private Integer totalActiveDays;
        private List<Map<String, Object>> recentSubmissions;
        private Map<String, Integer> languageStats;
        private LocalDateTime lastSyncAt;
        private boolean connected;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeetCodeConnectRequest {
        @NotBlank(message = "LeetCode username is required")
        private String username;
    }

    // ===== LEETGIT =====
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeetGitSyncRequest {
        @NotBlank(message = "Problem title is required")
        private String problemTitle;

        @NotBlank(message = "Problem slug is required")
        private String problemSlug;

        @NotBlank(message = "Language is required")
        private String language;

        @NotBlank(message = "Code is required")
        private String code;

        private String difficulty;
        private String problemDescription;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeetGitSyncResponse {
        private Long id;
        private String problemTitle;
        private String problemSlug;
        private String difficulty;
        private String language;
        private Integer aiScore;
        private String timeComplexity;
        private String spaceComplexity;
        private Map<String, Object> aiReview;
        private String optimizedCode;
        private String interviewNotes;
        private String githubCommitUrl;
        private LeetGitSync.SyncStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime syncedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeetGitStats {
        private Long totalSynced;
        private Long totalPushed;
        private Double syncSuccessRate;
        private LocalDateTime lastSyncAt;
        private List<LeetGitSyncResponse> recentSyncs;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnableLeetGitRequest {
        @NotBlank(message = "Repository name is required")
        private String repoName;
    }

    // ===== NEXUS OVERVIEW =====
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NexusOverviewResponse {
        private GitHubProfileResponse github;
        private LeetCodeProfileResponse leetcode;
        private LeetGitStats leetgit;
        private boolean githubConnected;
        private boolean leetcodeConnected;
        private boolean leetgitEnabled;
    }
}