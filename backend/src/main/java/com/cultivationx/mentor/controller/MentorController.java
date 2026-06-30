package com.cultivationx.mentor.controller;

import com.cultivationx.common.response.ApiResponse;
import com.cultivationx.mentor.dto.MentorDto;
import com.cultivationx.mentor.service.MentorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/mentor")
@RequiredArgsConstructor
public class MentorController {

    private final MentorService mentorService;

    // ===== CHAT =====
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<MentorDto.ChatResponse>> chat(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody MentorDto.ChatRequest request) {
        MentorDto.ChatResponse response = mentorService.chat(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<MentorDto.ConversationSummary>>> getConversations(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<MentorDto.ConversationSummary> response = mentorService.getConversations(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/conversations/{id}")
    public ResponseEntity<ApiResponse<MentorDto.ConversationDetail>> getConversation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        MentorDto.ConversationDetail response = mentorService.getConversation(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        mentorService.deleteConversation(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success("Conversation deleted"));
    }

    // ===== CODE REVIEW =====
    @PostMapping("/code-review")
    public ResponseEntity<ApiResponse<MentorDto.CodeReviewResponse>> reviewCode(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody MentorDto.CodeReviewRequest request) {
        MentorDto.CodeReviewResponse response = mentorService.reviewCode(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Code reviewed successfully", response));
    }

    @GetMapping("/code-review/history")
    public ResponseEntity<ApiResponse<List<MentorDto.CodeReviewSummary>>> getCodeReviewHistory(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<MentorDto.CodeReviewSummary> response = mentorService.getCodeReviewHistory(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}