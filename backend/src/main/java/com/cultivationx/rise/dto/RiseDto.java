package com.cultivationx.rise.dto;

import com.cultivationx.rise.entity.Resume;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class RiseDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumeResponse {
        private Long id;
        private String fileName;
        private String fileType;
        private Long fileSize;
        private Integer atsScore;
        private Integer keywordMatchScore;
        private Integer formattingScore;
        private Integer actionVerbScore;
        private Integer quantifiedAchievementsScore;
        private List<String> extractedSkills;
        private List<String> missingKeywords;
        private List<String> improvements;
        private List<String> strengths;
        private String experienceSummary;
        private String educationSummary;
        private String projectsSummary;
        private String overallFeedback;
        private Resume.ResumeStatus status;
        private boolean active;
        private Integer version;
        private LocalDateTime uploadedAt;
        private LocalDateTime analyzedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillGapResponse {
        private List<Map<String, Object>> existingSkills;
        private List<Map<String, Object>> missingSkills;
        private List<Map<String, Object>> weeklyRoadmap;
        private List<String> priorityAreas;
        private Integer estimatedReadinessWeeks;
        private Integer currentReadinessPercent;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LearningPathResponse {
        private Integer totalWeeks;
        private List<Map<String, Object>> phases;
        private Integer dailyHoursRequired;
        private List<String> weeklyGoals;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiseDashboardResponse {
        private ResumeResponse activeResume;
        private SkillGapResponse latestSkillGap;
        private Integer totalResumesUploaded;
        private Integer atsImprovement;   // compared to first upload
    }
}