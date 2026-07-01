package com.cultivationx.nexus.controller;

import com.cultivationx.common.response.ApiResponse;
import com.cultivationx.nexus.dto.NexusDto;
import com.cultivationx.nexus.service.GitHubService;
import com.cultivationx.nexus.service.LeetCodeService;
import com.cultivationx.nexus.service.LeetGitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/nexus")
@RequiredArgsConstructor
public class NexusController {

    private final GitHubService gitHubService;
    private final LeetCodeService leetCodeService;
    private final LeetGitService leetGitService;

    // ===== GITHUB =====
    @PostMapping("/github/token")
public ResponseEntity<ApiResponse<NexusDto.GitHubProfileResponse>> connectGitHubWithToken(
        @AuthenticationPrincipal UserDetails userDetails,
        @RequestBody Map<String, String> request) {
    String token = request.get("token");
    NexusDto.GitHubProfileResponse response = gitHubService.connectWithToken(
            userDetails.getUsername(), token);
    return ResponseEntity.ok(ApiResponse.success("GitHub connected successfully", response));
}

    @GetMapping("/github")
    public ResponseEntity<ApiResponse<NexusDto.GitHubProfileResponse>> getGitHubProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        NexusDto.GitHubProfileResponse response = gitHubService.getProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/github/sync")
    public ResponseEntity<ApiResponse<NexusDto.GitHubProfileResponse>> syncGitHub(
            @AuthenticationPrincipal UserDetails userDetails) {
        NexusDto.GitHubProfileResponse response = gitHubService.syncGitHub(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("GitHub synced successfully", response));
    }

    @DeleteMapping("/github/disconnect")
    public ResponseEntity<ApiResponse<Void>> disconnectGitHub(
            @AuthenticationPrincipal UserDetails userDetails) {
        gitHubService.disconnect(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("GitHub disconnected"));
    }

    // GitHub OAuth callback (public)
    @GetMapping("/github/callback")
    public ResponseEntity<ApiResponse<String>> githubCallback(@RequestParam String code) {
        return ResponseEntity.ok(ApiResponse.success("Use this code to connect", code));
    }

    // ===== LEETCODE =====
    @PostMapping("/leetcode/connect")
    public ResponseEntity<ApiResponse<NexusDto.LeetCodeProfileResponse>> connectLeetCode(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody NexusDto.LeetCodeConnectRequest request) {
        NexusDto.LeetCodeProfileResponse response = leetCodeService.connect(
                userDetails.getUsername(), request.getUsername());
        return ResponseEntity.ok(ApiResponse.success("LeetCode connected successfully", response));
    }

    @GetMapping("/leetcode")
    public ResponseEntity<ApiResponse<NexusDto.LeetCodeProfileResponse>> getLeetCodeProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        NexusDto.LeetCodeProfileResponse response = leetCodeService.getProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/leetcode/sync")
    public ResponseEntity<ApiResponse<NexusDto.LeetCodeProfileResponse>> syncLeetCode(
            @AuthenticationPrincipal UserDetails userDetails) {
        NexusDto.LeetCodeProfileResponse response = leetCodeService.sync(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("LeetCode synced successfully", response));
    }

    @DeleteMapping("/leetcode/disconnect")
    public ResponseEntity<ApiResponse<Void>> disconnectLeetCode(
            @AuthenticationPrincipal UserDetails userDetails) {
        leetCodeService.disconnect(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("LeetCode disconnected"));
    }

    // ===== LEETGIT =====
    @PostMapping("/leetgit/enable")
    public ResponseEntity<ApiResponse<Void>> enableLeetGit(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody NexusDto.EnableLeetGitRequest request) {
        leetGitService.enableLeetGit(userDetails.getUsername(), request.getRepoName());
        return ResponseEntity.ok(ApiResponse.success("LeetGit enabled. Repository: " + request.getRepoName()));
    }

    @PostMapping("/leetgit/sync")
    public ResponseEntity<ApiResponse<NexusDto.LeetGitSyncResponse>> syncSolution(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody NexusDto.LeetGitSyncRequest request) {
        NexusDto.LeetGitSyncResponse response = leetGitService.syncSolution(
                userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Solution synced to GitHub with AI review", response));
    }

    @GetMapping("/leetgit/stats")
    public ResponseEntity<ApiResponse<NexusDto.LeetGitStats>> getLeetGitStats(
            @AuthenticationPrincipal UserDetails userDetails) {
        NexusDto.LeetGitStats response = leetGitService.getStats(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/leetgit/history")
    public ResponseEntity<ApiResponse<List<NexusDto.LeetGitSyncResponse>>> getLeetGitHistory(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<NexusDto.LeetGitSyncResponse> response = leetGitService.getHistory(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
