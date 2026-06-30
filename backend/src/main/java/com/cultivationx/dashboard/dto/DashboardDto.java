package com.cultivationx.dashboard.dto;

import com.cultivationx.auth.dto.AuthDto;
import lombok.*;

import java.util.List;

public class DashboardDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardResponse {
        private String greeting;
        private AuthDto.UserResponse user;
        private int devDnaScore;
        private int interviewReadinessScore;
        private int currentStreak;
        private int atsScore;
        private int leetcodeSolved;
        private int githubRepos;
        private int githubContributions;
        private int leetgitSynced;           // ← changed from leethubSynced
        private List<ActivityItem> recentActivity;
        private List<TaskItem> todaysTasks;
        private List<ProgressItem> progress;
        private boolean githubConnected;
        private boolean leetcodeConnected;
        private boolean leetgitEnabled;      // ← changed from leethubEnabled
        private boolean setupComplete;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityItem {
        private String icon;
        private String description;
        private String time;
        private String type;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskItem {
        private String task;
        private String impact;
        private String priority;
        private boolean completed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressItem {
        private String label;
        private int value;
        private String color;
    }
}