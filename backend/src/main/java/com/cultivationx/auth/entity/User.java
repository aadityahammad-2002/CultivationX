package com.cultivationx.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== BASIC AUTH =====
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Builder.Default
    private boolean enabled = true;

    // ===== PROFILE =====
    private String bio;
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Goal goal = Goal.SDE_1;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ExperienceLevel experienceLevel = ExperienceLevel.FRESHER;

    // ===== SETUP WIZARD =====
    @Builder.Default
    private boolean setupComplete = false;

    private String targetCompany;
    private String currentRole;
    private Integer yearsOfExperience;

    // ===== GITHUB INTEGRATION (Nexus) =====
    private String githubUsername;
    private String githubAccessToken;       // stored encrypted
    private String githubRefreshToken;
    private LocalDateTime githubConnectedAt;
    private LocalDateTime githubLastSyncAt;

    @Builder.Default
    private boolean githubConnected = false;

    // ===== LEETCODE INTEGRATION (Nexus) =====
    private String leetcodeUsername;
    private LocalDateTime leetcodeConnectedAt;
    private LocalDateTime leetcodeLastSyncAt;

    @Builder.Default
    private boolean leetcodeConnected = false;

    // ===== LEETGIT INTEGRATION (Nexus) =====
    @Column(name = "leethub_repo_name")
    private String leetgitRepoName;

    @Column(name = "leethub_last_sync_at")
    private LocalDateTime leetgitLastSyncAt;

    @Column(name = "leethub_enabled")
    @Builder.Default
    private boolean leetgitEnabled = false;

    // ===== DEV DNA SCORE =====
    @Builder.Default
    private Integer devDnaScore = 0;

    @Builder.Default
    private Integer interviewReadinessScore = 0;

    // ===== STREAK & GAMIFICATION =====
    @Builder.Default
    private Integer currentStreak = 0;

    @Builder.Default
    private Integer longestStreak = 0;

    private LocalDateTime lastActivityDate;

    // ===== TIMESTAMPS =====
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ===== UserDetails implementation =====
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return enabled; }

    // ===== ENUMS =====
    public enum Goal {
        INTERNSHIP, SDE_1, FAANG, PRODUCT_COMPANY, SERVICE_COMPANY
    }

    public enum ExperienceLevel {
        FRESHER, JUNIOR, MID, SENIOR
    }
}