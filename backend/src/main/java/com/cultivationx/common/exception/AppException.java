package com.cultivationx.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AppException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public AppException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.code = status.name();
    }

    public AppException(String message, HttpStatus status, String code) {
        super(message);
        this.status = status;
        this.code = code;
    }

    // ===== AUTH =====
    public static AppException userNotFound(String email) {
        return new AppException("User not found: " + email, HttpStatus.NOT_FOUND, "USER_NOT_FOUND");
    }

    public static AppException userNotFoundById(Long id) {
        return new AppException("User not found with id: " + id, HttpStatus.NOT_FOUND, "USER_NOT_FOUND");
    }

    public static AppException emailAlreadyExists(String email) {
        return new AppException("Email already registered: " + email, HttpStatus.CONFLICT, "EMAIL_EXISTS");
    }

    public static AppException invalidCredentials() {
        return new AppException("Invalid email or password", HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
    }

    public static AppException invalidToken() {
        return new AppException("Invalid or expired token", HttpStatus.UNAUTHORIZED, "INVALID_TOKEN");
    }

    public static AppException unauthorized() {
        return new AppException("Unauthorized access", HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }

    // ===== RISE =====
    public static AppException resumeNotFound(Long userId) {
        return new AppException("No resume found for user: " + userId, HttpStatus.NOT_FOUND, "RESUME_NOT_FOUND");
    }

    public static AppException invalidFileType(String type) {
        return new AppException("Unsupported file type: " + type + ". Only PDF and DOCX are allowed.", HttpStatus.BAD_REQUEST, "INVALID_FILE_TYPE");
    }

    public static AppException fileTooLarge(long sizeMb) {
        return new AppException("File size " + sizeMb + "MB exceeds limit of 10MB", HttpStatus.BAD_REQUEST, "FILE_TOO_LARGE");
    }

    public static AppException resumeParseError(String detail) {
        return new AppException("Failed to parse resume: " + detail, HttpStatus.INTERNAL_SERVER_ERROR, "RESUME_PARSE_ERROR");
    }

    // ===== NEXUS =====
    public static AppException githubNotConnected() {
        return new AppException("GitHub account not connected. Please connect GitHub first.", HttpStatus.BAD_REQUEST, "GITHUB_NOT_CONNECTED");
    }

    public static AppException leetcodeNotConnected() {
        return new AppException("LeetCode account not connected. Please connect LeetCode first.", HttpStatus.BAD_REQUEST, "LEETCODE_NOT_CONNECTED");
    }

    public static AppException githubApiError(String detail) {
        return new AppException("GitHub API error: " + detail, HttpStatus.SERVICE_UNAVAILABLE, "GITHUB_API_ERROR");
    }

    public static AppException leetcodeApiError(String detail) {
        return new AppException("LeetCode API error: " + detail, HttpStatus.SERVICE_UNAVAILABLE, "LEETCODE_API_ERROR");
    }

    // ===== MENTOR =====
    public static AppException conversationNotFound(Long id) {
        return new AppException("Conversation not found: " + id, HttpStatus.NOT_FOUND, "CONVERSATION_NOT_FOUND");
    }

    public static AppException rateLimitExceeded() {
        return new AppException("AI request limit exceeded. Please wait before sending another message.", HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED");
    }

    // ===== AI =====
    public static AppException aiServiceError(String detail) {
        return new AppException("AI service error: " + detail, HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_ERROR");
    }

    // ===== GENERIC =====
    public static AppException notFound(String resource) {
        return new AppException(resource + " not found", HttpStatus.NOT_FOUND, "NOT_FOUND");
    }

    public static AppException badRequest(String message) {
        return new AppException(message, HttpStatus.BAD_REQUEST, "BAD_REQUEST");
    }

    public static AppException internalError(String message) {
        return new AppException(message, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR");
    }
}