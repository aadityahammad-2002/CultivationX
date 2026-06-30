package com.cultivationx.dashboard.service;

import com.cultivationx.auth.dto.AuthDto;
import com.cultivationx.auth.entity.User;
import com.cultivationx.auth.repository.UserRepository;
import com.cultivationx.common.exception.AppException;
import com.cultivationx.dashboard.dto.DashboardDto;
import com.cultivationx.devdna.repository.DevDnaReportRepository;
import com.cultivationx.nexus.repository.GitHubProfileRepository;
import com.cultivationx.nexus.repository.LeetCodeProfileRepository;
import com.cultivationx.nexus.repository.LeetGitSyncRepository;
import com.cultivationx.rise.repository.ResumeRepository;
import com.cultivationx.rise.repository.SkillGapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final GitHubProfileRepository gitHubProfileRepository;
    private final LeetCodeProfileRepository leetCodeProfileRepository;
    private final LeetGitSyncRepository leetGitSyncRepository;
    private final DevDnaReportRepository devDnaReportRepository;
    private final SkillGapRepository skillGapRepository;

    public DashboardDto.DashboardResponse getDashboard(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.userNotFound(email));

        // Quick stats
        Integer atsScore = resumeRepository.findByUserIdAndActiveTrue(user.getId())
                .map(r -> r.getAtsScore() != null ? r.getAtsScore() : 0).orElse(0);

        Integer leetcodeSolved = leetCodeProfileRepository.findByUserId(user.getId())
                .map(p -> p.getTotalSolved() != null ? p.getTotalSolved() : 0).orElse(0);

        Integer githubRepos = gitHubProfileRepository.findByUserId(user.getId())
                .map(p -> p.getPublicRepos() != null ? p.getPublicRepos() : 0).orElse(0);

        Integer githubContributions = gitHubProfileRepository.findByUserId(user.getId())
                .map(p -> p.getTotalCommitsThisYear() != null ? p.getTotalCommitsThisYear() : 0).orElse(0);

        long leetgitSynced = leetGitSyncRepository.countSyncedByUserId(user.getId());

        // Recent activity
        List<DashboardDto.ActivityItem> recentActivity = buildRecentActivity(user);

        // Today's tasks based on what's missing
        List<DashboardDto.TaskItem> todaysTasks = buildTodaysTasks(user, atsScore, leetcodeSolved);

        // Progress bars
        List<DashboardDto.ProgressItem> progress = List.of(
                DashboardDto.ProgressItem.builder()
                        .label("GitHub").value(Math.min(githubRepos * 5, 100)).color("#6c63ff").build(),
                DashboardDto.ProgressItem.builder()
                        .label("LeetCode").value(Math.min(leetcodeSolved / 5, 100)).color("#a78bfa").build(),
                DashboardDto.ProgressItem.builder()
                        .label("Resume").value(atsScore).color("#818cf8").build(),
                DashboardDto.ProgressItem.builder()
                        .label("LeetGit").value((int) Math.min(leetgitSynced * 5, 100)).color("#c4b5fd").build()
        );

        // Greeting
        int hour = LocalDateTime.now().getHour();
        String timeOfDay = hour < 12 ? "Morning" : hour < 17 ? "Afternoon" : "Evening";
        String greeting = "Good " + timeOfDay + ", " + user.getName().split(" ")[0] + "!";

        return DashboardDto.DashboardResponse.builder()
                .greeting(greeting)
                .user(AuthDto.UserResponse.from(user))
                .devDnaScore(user.getDevDnaScore() != null ? user.getDevDnaScore() : 0)
                .interviewReadinessScore(user.getInterviewReadinessScore() != null ? user.getInterviewReadinessScore() : 0)
                .currentStreak(user.getCurrentStreak() != null ? user.getCurrentStreak() : 0)
                .atsScore(atsScore)
                .leetcodeSolved(leetcodeSolved)
                .githubRepos(githubRepos)
                .githubContributions(githubContributions)
                .leetgitSynced((int) leetgitSynced)
                .recentActivity(recentActivity)
                .todaysTasks(todaysTasks)
                .progress(progress)
                .githubConnected(user.isGithubConnected())
                .leetcodeConnected(user.isLeetcodeConnected())
                .leetgitEnabled(user.isLeetgitEnabled())
                .setupComplete(user.isSetupComplete())
                .build();
    }

    private List<DashboardDto.ActivityItem> buildRecentActivity(User user) {
        List<DashboardDto.ActivityItem> activities = new ArrayList<>();

        // LeetGit syncs
        leetGitSyncRepository.findByUserIdOrderByCreatedAtDesc(user.getId(),
                        org.springframework.data.domain.PageRequest.of(0, 3))
                .forEach(sync -> activities.add(DashboardDto.ActivityItem.builder()
                        .icon("✓")
                        .description(sync.getProblemTitle() + " synced to GitHub")
                        .time(formatTime(sync.getCreatedAt()))
                        .type("leetgit")
                        .build()));

        // Resume upload
        resumeRepository.findTopByUserIdOrderByUploadedAtDesc(user.getId())
                .ifPresent(r -> activities.add(DashboardDto.ActivityItem.builder()
                        .icon("📄")
                        .description("Resume ATS score: " + r.getAtsScore() + "/100")
                        .time(formatTime(r.getUploadedAt()))
                        .type("resume")
                        .build()));

        return activities;
    }

    private List<DashboardDto.TaskItem> buildTodaysTasks(User user, int atsScore, int leetcodeSolved) {
        List<DashboardDto.TaskItem> tasks = new ArrayList<>();

        if (!user.isGithubConnected()) {
            tasks.add(DashboardDto.TaskItem.builder()
                    .task("Connect your GitHub account").impact("+10 Dev DNA")
                    .priority("high").completed(false).build());
        }
        if (!user.isLeetcodeConnected()) {
            tasks.add(DashboardDto.TaskItem.builder()
                    .task("Connect your LeetCode account").impact("+10 Dev DNA")
                    .priority("high").completed(false).build());
        }
        if (atsScore == 0) {
            tasks.add(DashboardDto.TaskItem.builder()
                    .task("Upload your resume for ATS analysis").impact("+15 Dev DNA")
                    .priority("high").completed(false).build());
        } else if (atsScore < 80) {
            tasks.add(DashboardDto.TaskItem.builder()
                    .task("Improve resume ATS score (currently " + atsScore + "/100)")
                    .impact("+5 Dev DNA").priority("medium").completed(false).build());
        }
        tasks.add(DashboardDto.TaskItem.builder()
                .task("Solve 2 Medium LeetCode problems").impact("+3 Dev DNA")
                .priority("medium").completed(false).build());
        tasks.add(DashboardDto.TaskItem.builder()
                .task("Push a GitHub commit today").impact("+2 Dev DNA")
                .priority("low").completed(false).build());

        return tasks.stream().limit(4).toList();
    }

    private String formatTime(LocalDateTime time) {
        if (time == null) return "";
        LocalDateTime now = LocalDateTime.now();
        long hours = java.time.Duration.between(time, now).toHours();
        if (hours < 1) return "Just now";
        if (hours < 24) return hours + " hours ago";
        return time.format(DateTimeFormatter.ofPattern("dd MMM"));
    }
}