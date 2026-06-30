package com.cultivationx.rise.controller;

import com.cultivationx.common.response.ApiResponse;
import com.cultivationx.rise.dto.RiseDto;
import com.cultivationx.rise.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/rise")
@RequiredArgsConstructor
public class RiseController {

    private final ResumeService resumeService;

    @PostMapping("/resume/upload")
    public ResponseEntity<ApiResponse<RiseDto.ResumeResponse>> uploadResume(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) {
        RiseDto.ResumeResponse response = resumeService.uploadAndAnalyze(userDetails.getUsername(), file);
        return ResponseEntity.ok(ApiResponse.success("Resume uploaded and analyzed", response));
    }

    @GetMapping("/resume")
    public ResponseEntity<ApiResponse<RiseDto.ResumeResponse>> getActiveResume(
            @AuthenticationPrincipal UserDetails userDetails) {
        RiseDto.ResumeResponse response = resumeService.getActiveResume(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/resume/reanalyze")
    public ResponseEntity<ApiResponse<RiseDto.ResumeResponse>> reanalyze(
            @AuthenticationPrincipal UserDetails userDetails) {
        RiseDto.ResumeResponse response = resumeService.reanalyze(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Resume reanalyzed", response));
    }

    @GetMapping("/resume/history")
    public ResponseEntity<ApiResponse<List<RiseDto.ResumeResponse>>> getHistory(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<RiseDto.ResumeResponse> response = resumeService.getHistory(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/skill-gap")
    public ResponseEntity<ApiResponse<RiseDto.SkillGapResponse>> getSkillGap(
            @AuthenticationPrincipal UserDetails userDetails) {
        RiseDto.SkillGapResponse response = resumeService.getSkillGap(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<RiseDto.RiseDashboardResponse>> getDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        RiseDto.RiseDashboardResponse response = resumeService.getDashboard(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}