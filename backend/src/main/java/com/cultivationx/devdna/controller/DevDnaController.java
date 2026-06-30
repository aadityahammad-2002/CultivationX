package com.cultivationx.devdna.controller;

import com.cultivationx.common.response.ApiResponse;
import com.cultivationx.devdna.dto.DevDnaDto;
import com.cultivationx.devdna.service.DevDnaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/devdna")
@RequiredArgsConstructor
public class DevDnaController {

    private final DevDnaService devDnaService;

    @GetMapping
    public ResponseEntity<ApiResponse<DevDnaDto.DevDnaReportResponse>> getLatestReport(
            @AuthenticationPrincipal UserDetails userDetails) {
        DevDnaDto.DevDnaReportResponse response = devDnaService.getLatestReport(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<DevDnaDto.DevDnaReportResponse>> generateReport(
            @AuthenticationPrincipal UserDetails userDetails) {
        DevDnaDto.DevDnaReportResponse response = devDnaService.generateReport(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Dev DNA report generated", response));
    }

    @GetMapping("/weekly")
    public ResponseEntity<ApiResponse<DevDnaDto.WeeklyReportResponse>> getWeeklyReport(
            @AuthenticationPrincipal UserDetails userDetails) {
        DevDnaDto.WeeklyReportResponse response = devDnaService.getWeeklyReport(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<DevDnaDto.GrowthHistoryResponse>> getGrowthHistory(
            @AuthenticationPrincipal UserDetails userDetails) {
        DevDnaDto.GrowthHistoryResponse response = devDnaService.getGrowthHistory(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}