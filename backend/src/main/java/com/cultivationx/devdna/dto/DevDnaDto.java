package com.cultivationx.devdna.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class DevDnaDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DevDnaReportResponse {
        private Integer devDnaScore;
        private Integer interviewReadinessScore;
        private ScoreBreakdown scoreBreakdown;
        private List<Map<String, Object>> skillGraph;
        private List<String> strengths;
        private List<String> areasToImprove;
        private List<String> insights;
        private Map<String, Object> weeklyRoadmap;
        private Map<String, Object> interviewReadiness;
        private Map<String, Object> todaysMission;
        private String profileSummary;
        private String motivationalMessage;
        private LocalDateTime generatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreBreakdown {
        private Integer github;
        private Integer leetcode;
        private Integer leetgit;
        private Integer resume;
        private Integer consistency;
        private Integer aiAssessment;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklyReportResponse {
        private String headline;
        private String overallProgress;
        private List<String> strengths;
        private List<String> weaknesses;
        private List<String> insights;
        private Map<String, Object> todaysMission;
        private List<Map<String, Object>> weeklyGoals;
        private Map<String, Object> interviewReadiness;
        private String motivationalMessage;
        private LocalDateTime generatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GrowthHistoryResponse {
        private List<ScorePoint> scoreHistory;
        private Integer totalImprovement;
        private String trendDescription;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScorePoint {
        private Integer score;
        private Integer interviewReadiness;
        private LocalDateTime date;
    }
}