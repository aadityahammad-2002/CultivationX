package com.cultivationx.mentor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class MentorDto {

    // ===== CHAT =====
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatRequest {
        @NotBlank(message = "Message cannot be empty")
        @Size(max = 4000, message = "Message too long")
        private String message;

        private Long conversationId;  // null = new conversation
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatResponse {
        private Long conversationId;
        private String conversationTitle;
        private String reply;
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConversationSummary {
        private Long id;
        private String title;
        private String lastMessage;
        private LocalDateTime lastMessageAt;
        private int messageCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConversationDetail {
        private Long id;
        private String title;
        private List<MessageDto> messages;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageDto {
        private Long id;
        private String role;
        private String content;
        private LocalDateTime createdAt;
    }

    // ===== CODE REVIEW =====
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CodeReviewRequest {
        @NotBlank(message = "Code is required")
        private String code;

        @NotBlank(message = "Language is required")
        private String language;

        private String problemContext;

        private boolean requestBetterSolution;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CodeReviewResponse {
        private Long id;
        private String language;
        private String problemContext;
        private Integer overallScore;
        private String grade;
        private Map<String, Object> review;
        private Map<String, Object> betterSolution;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CodeReviewSummary {
        private Long id;
        private String language;
        private String problemContext;
        private Integer overallScore;
        private String grade;
        private LocalDateTime createdAt;
    }
}