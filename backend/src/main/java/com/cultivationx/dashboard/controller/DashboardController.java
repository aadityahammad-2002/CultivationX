package com.cultivationx.dashboard.controller;

import com.cultivationx.common.response.ApiResponse;
import com.cultivationx.dashboard.dto.DashboardDto;
import com.cultivationx.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<DashboardDto.DashboardResponse>> getDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        DashboardDto.DashboardResponse response = dashboardService.getDashboard(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}