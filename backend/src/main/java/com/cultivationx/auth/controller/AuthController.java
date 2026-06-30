package com.cultivationx.auth.controller;

import com.cultivationx.auth.dto.AuthDto;
import com.cultivationx.auth.service.AuthService;
import com.cultivationx.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthDto.AuthResponse>> register(
            @Valid @RequestBody AuthDto.RegisterRequest request) {
        AuthDto.AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDto.AuthResponse>> login(
            @Valid @RequestBody AuthDto.LoginRequest request) {
        AuthDto.AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthDto.UserResponse>> getMe(
            @AuthenticationPrincipal UserDetails userDetails) {
        AuthDto.UserResponse response = authService.getMe(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/setup")
    public ResponseEntity<ApiResponse<AuthDto.UserResponse>> completeSetup(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AuthDto.SetupRequest request) {
        AuthDto.UserResponse response = authService.completeSetup(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Setup complete", response));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<AuthDto.UserResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody AuthDto.ProfileUpdateRequest request) {
        AuthDto.UserResponse response = authService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", response));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AuthDto.ChangePasswordRequest request) {
        authService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }
}