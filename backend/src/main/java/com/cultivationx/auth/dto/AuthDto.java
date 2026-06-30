package com.cultivationx.auth.dto;

import com.cultivationx.auth.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class AuthDto {

    // ===== REGISTER =====
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest {
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        private String name;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;
    }

    // ===== LOGIN =====
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;
    }

    // ===== AUTH RESPONSE =====
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthResponse {
        private String token;
        private UserResponse user;
    }

    // ===== USER RESPONSE (safe - no password) =====
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserResponse {
        private Long id;
        private String name;
        private String email;
        private String bio;
        private String avatarUrl;
        private User.Goal goal;
        private User.ExperienceLevel experienceLevel;
        private String targetCompany;
        private String currentRole;
        private Integer yearsOfExperience;
        private boolean setupComplete;
        private boolean githubConnected;
        private boolean leetcodeConnected;
        private boolean leethubEnabled;
        private String githubUsername;
        private String leetcodeUsername;
        private String leethubRepoName;
        private Integer devDnaScore;
        private Integer interviewReadinessScore;
        private Integer currentStreak;
        private Integer longestStreak;
        private LocalDateTime createdAt;

        public static UserResponse from(User user) {
            return UserResponse.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .bio(user.getBio())
                    .avatarUrl(user.getAvatarUrl())
                    .goal(user.getGoal())
                    .experienceLevel(user.getExperienceLevel())
                    .targetCompany(user.getTargetCompany())
                    .currentRole(user.getCurrentRole())
                    .yearsOfExperience(user.getYearsOfExperience())
                    .setupComplete(user.isSetupComplete())
                    .githubConnected(user.isGithubConnected())
                    .leetcodeConnected(user.isLeetcodeConnected())
                    .leethubEnabled(user.isLeetgitEnabled())
                    .githubUsername(user.getGithubUsername())
                    .leetcodeUsername(user.getLeetcodeUsername())
                    .leethubRepoName(user.getLeetgitRepoName())
                    .devDnaScore(user.getDevDnaScore())
                    .interviewReadinessScore(user.getInterviewReadinessScore())
                    .currentStreak(user.getCurrentStreak())
                    .longestStreak(user.getLongestStreak())
                    .createdAt(user.getCreatedAt())
                    .build();
        }
    }

    // ===== SETUP WIZARD =====
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SetupRequest {
        @NotNull(message = "Goal is required")
        private User.Goal goal;

        private User.ExperienceLevel experienceLevel;
        private String targetCompany;
        private String currentRole;
        private Integer yearsOfExperience;
        private String leetcodeUsername;
        private String bio;
    }

    // ===== PROFILE UPDATE =====
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileUpdateRequest {
        @Size(min = 2, max = 100)
        private String name;
        private String bio;
        private User.Goal goal;
        private User.ExperienceLevel experienceLevel;
        private String targetCompany;
        private String currentRole;
        private Integer yearsOfExperience;
    }

    // ===== CHANGE PASSWORD =====
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangePasswordRequest {
        @NotBlank
        private String currentPassword;

        @NotBlank
        @Size(min = 8, message = "New password must be at least 8 characters")
        private String newPassword;
    }
}