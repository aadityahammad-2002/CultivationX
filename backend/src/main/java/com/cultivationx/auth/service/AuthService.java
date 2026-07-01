package com.cultivationx.auth.service;

import com.cultivationx.auth.dto.AuthDto;
import com.cultivationx.auth.entity.User;
import com.cultivationx.auth.repository.UserRepository;
import com.cultivationx.common.exception.AppException;
import com.cultivationx.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Transactional
public AuthDto.AuthResponse register(AuthDto.RegisterRequest request) {
    log.info("=== REGISTER START ===");
    log.info("Name: {}, Email: {}", request.getName(), request.getEmail());
    
    try {
        log.info("Checking if email exists...");
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Email already exists: {}", request.getEmail());
            throw AppException.emailAlreadyExists(request.getEmail());
        }
        
        log.info("Creating user...");
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .setupComplete(false)
                .build();

        log.info("Saving user...");
        user = userRepository.save(user);
        log.info("User saved with ID: {}", user.getId());
        
        log.info("Generating JWT token...");
        String token = jwtUtil.generateToken(user);
        log.info("Token generated");
        
        return AuthDto.AuthResponse.builder()
                .token(token)
                .user(AuthDto.UserResponse.from(user))
                .build();
    } catch (AppException e) {
        log.error("AppException: {}", e.getMessage());
        throw e;
    } catch (Exception e) {
        log.error("UNEXPECTED ERROR in register: ", e);
        throw e;
    }
}

    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> AppException.userNotFound(request.getEmail()));

        String token = jwtUtil.generateToken(user);
        log.info("User logged in: {}", user.getEmail());

        return AuthDto.AuthResponse.builder()
                .token(token)
                .user(AuthDto.UserResponse.from(user))
                .build();
    }

    public AuthDto.UserResponse getMe(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.userNotFound(email));
        return AuthDto.UserResponse.from(user);
    }

    @Transactional
    public AuthDto.UserResponse completeSetup(String email, AuthDto.SetupRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.userNotFound(email));

        user.setGoal(request.getGoal());
        if (request.getExperienceLevel() != null) user.setExperienceLevel(request.getExperienceLevel());
        if (request.getTargetCompany() != null) user.setTargetCompany(request.getTargetCompany());
        if (request.getCurrentRole() != null) user.setCurrentRole(request.getCurrentRole());
        if (request.getYearsOfExperience() != null) user.setYearsOfExperience(request.getYearsOfExperience());
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getLeetcodeUsername() != null) {
            user.setLeetcodeUsername(request.getLeetcodeUsername());
            user.setLeetcodeConnected(true);
        }
        user.setSetupComplete(true);

        user = userRepository.save(user);
        log.info("Setup complete for user: {}", user.getEmail());
        return AuthDto.UserResponse.from(user);
    }

    @Transactional
    public AuthDto.UserResponse updateProfile(String email, AuthDto.ProfileUpdateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.userNotFound(email));

        if (request.getName() != null) user.setName(request.getName());
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getGoal() != null) user.setGoal(request.getGoal());
        if (request.getExperienceLevel() != null) user.setExperienceLevel(request.getExperienceLevel());
        if (request.getTargetCompany() != null) user.setTargetCompany(request.getTargetCompany());
        if (request.getCurrentRole() != null) user.setCurrentRole(request.getCurrentRole());
        if (request.getYearsOfExperience() != null) user.setYearsOfExperience(request.getYearsOfExperience());

        user = userRepository.save(user);
        return AuthDto.UserResponse.from(user);
    }

    @Transactional
    public void changePassword(String email, AuthDto.ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.userNotFound(email));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw AppException.invalidCredentials();
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user: {}", email);
    }
}
