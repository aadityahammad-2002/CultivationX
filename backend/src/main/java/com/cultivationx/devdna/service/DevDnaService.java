package com.cultivationx.devdna.service;

import com.cultivationx.ai.service.AiService;
import com.cultivationx.auth.entity.User;
import com.cultivationx.auth.repository.UserRepository;
import com.cultivationx.common.exception.AppException;
import com.cultivationx.devdna.dto.DevDnaDto;
import com.cultivationx.devdna.entity.DevDnaReport;
import com.cultivationx.devdna.repository.DevDnaReportRepository;
import com.cultivationx.nexus.entity.GitHubProfile;
import com.cultivationx.nexus.entity.LeetCodeProfile;
import com.cultivationx.nexus.repository.GitHubProfileRepository;
import com.cultivationx.nexus.repository.LeetCodeProfileRepository;
import com.cultivationx.nexus.repository.LeetGitSyncRepository;
import com.cultivationx.rise.repository.ResumeRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DevDnaService {

    private final DevDnaReportRepository devDnaReportRepository;
    private final UserRepository userRepository;
    private final GitHubProfileRepository gitHubProfileRepository;
    private final LeetCodeProfileRepository leetCodeProfileRepository;
    private final LeetGitSyncRepository leetGitSyncRepository;
    private final ResumeRepository resumeRepository;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    @Transactional
    public DevDnaDto.DevDnaReportResponse generateReport(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.userNotFound(email));

        String githubData = buildGitHubSummary(user);
        String leetcodeData = buildLeetCodeSummary(user);
        String resumeData = buildResumeSummary(user);

        Map<String, Object> aiAnalysis = aiService.generateDevDnaAnalysis(
                user.getName(), user.getGoal() != null ? user.getGoal().name() : "SDE_1",
                githubData, leetcodeData, resumeData
        );

        int devDnaScore = toInt(aiAnalysis.get("devDnaScore"));
        int interviewScore = toInt(aiAnalysis.get("interviewReadinessScore"));

        // Compute component scores
        int githubScore = computeGitHubScore(user);
        int leetcodeScore = computeLeetCodeScore(user);
        int leetgitScore = computeLeetGitScore(user);
        int resumeScore = computeResumeScore(user);
        int consistencyScore = computeConsistencyScore(user);
        int aiScore = interviewScore;

        DevDnaReport report = DevDnaReport.builder()
                .user(user)
                .devDnaScore(devDnaScore)
                .interviewReadinessScore(interviewScore)
                .githubScore(githubScore)
                .leetcodeScore(leetcodeScore)
                .leetgitScore(leetgitScore)
                .resumeScore(resumeScore)
                .consistencyScore(consistencyScore)
                .aiAssessmentScore(aiScore)
                .skillGraphJson(toJson(aiAnalysis.get("skillGraph")))
                .strengthsJson(toJson(aiAnalysis.get("topStrengths")))
                .areasToImproveJson(toJson(aiAnalysis.get("areasToImprove")))
                .weeklyRoadmapJson(toJson(aiAnalysis.get("monthlyRoadmap")))
                .profileSummary(toString(aiAnalysis.get("profileSummary")))
                .build();

        devDnaReportRepository.save(report);

        // Update user scores
        userRepository.updateDevDnaScore(user.getId(), devDnaScore, LocalDateTime.now());
        userRepository.updateInterviewReadiness(user.getId(), interviewScore, LocalDateTime.now());

        return toResponse(report);
    }

    public DevDnaDto.DevDnaReportResponse getLatestReport(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.userNotFound(email));

        return devDnaReportRepository.findTopByUserIdOrderByGeneratedAtDesc(user.getId())
                .map(this::toResponse)
                .orElseGet(() -> generateReport(email));
    }

    public DevDnaDto.WeeklyReportResponse getWeeklyReport(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.userNotFound(email));

        String githubStats = buildGitHubSummary(user);
        String leetcodeStats = buildLeetCodeSummary(user);
        String resumeStats = buildResumeSummary(user);

        Map<String, Object> report = aiService.generateWeeklyReport(
                user.getName(),
                user.getDevDnaScore() != null ? user.getDevDnaScore() : 0,
                githubStats, leetcodeStats, resumeStats,
                user.getGoal() != null ? user.getGoal().name() : "SDE_1"
        );

        return DevDnaDto.WeeklyReportResponse.builder()
                .headline(toString(report.get("headline")))
                .overallProgress(toString(report.get("overallProgress")))
                .strengths(fromJsonList(toJson(report.get("strengths"))))
                .weaknesses(fromJsonList(toJson(report.get("weaknesses"))))
                .insights(fromJsonList(toJson(report.get("insights"))))
                .todaysMission(safeGetMap(report, "todaysMission"))
                .weeklyGoals(fromJsonMapList(toJson(report.get("weeklyGoals"))))
                .interviewReadiness(safeGetMap(report, "interviewReadiness"))
                .motivationalMessage(toString(report.get("motivationalMessage")))
                .generatedAt(LocalDateTime.now())
                .build();
    }

    public DevDnaDto.GrowthHistoryResponse getGrowthHistory(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.userNotFound(email));

        List<DevDnaDto.ScorePoint> history = devDnaReportRepository
                .findByUserIdOrderByGeneratedAtDesc(user.getId(), PageRequest.of(0, 30))
                .stream()
                .map(r -> DevDnaDto.ScorePoint.builder()
                        .score(r.getDevDnaScore())
                        .interviewReadiness(r.getInterviewReadinessScore())
                        .date(r.getGeneratedAt())
                        .build())
                .toList();

        int improvement = 0;
        if (history.size() >= 2) {
            improvement = history.get(0).getScore() - history.get(history.size() - 1).getScore();
        }

        return DevDnaDto.GrowthHistoryResponse.builder()
                .scoreHistory(history)
                .totalImprovement(improvement)
                .trendDescription(improvement > 0 ? "Improving" : improvement < 0 ? "Declining" : "Stable")
                .build();
    }

    // Weekly regeneration
    @Scheduled(cron = "${app.scheduling.devdna-cron:0 0 0 * * SUN}")
    public void regenerateAllReports() {
        log.info("Starting weekly Dev DNA regeneration...");
        userRepository.findAll().forEach(user -> {
            try {
                generateReport(user.getEmail());
            } catch (Exception e) {
                log.error("Dev DNA regen failed for {}: {}", user.getEmail(), e.getMessage());
            }
        });
    }

    // ===== SCORE COMPUTATION =====

    private int computeGitHubScore(User user) {
        if (!user.isGithubConnected()) return 0;
        Optional<GitHubProfile> profile = gitHubProfileRepository.findByUserId(user.getId());
        return profile.map(p -> {
            int score = 0;
            if (p.getPublicRepos() != null) score += Math.min(p.getPublicRepos() * 2, 40);
            if (p.getTotalStars() != null) score += Math.min(p.getTotalStars() * 5, 30);
            if (p.getFollowers() != null) score += Math.min(p.getFollowers(), 15);
            if (p.getCurrentStreak() != null) score += Math.min(p.getCurrentStreak(), 15);
            return Math.min(score, 100);
        }).orElse(0);
    }

    private int computeLeetCodeScore(User user) {
        if (!user.isLeetcodeConnected()) return 0;
        Optional<LeetCodeProfile> profile = leetCodeProfileRepository.findByUserId(user.getId());
        return profile.map(p -> {
            int score = 0;
            if (p.getEasySolved() != null) score += Math.min(p.getEasySolved() / 2, 20);
            if (p.getMediumSolved() != null) score += Math.min(p.getMediumSolved(), 40);
            if (p.getHardSolved() != null) score += Math.min(p.getHardSolved() * 3, 30);
            if (p.getCurrentStreak() != null) score += Math.min(p.getCurrentStreak(), 10);
            return Math.min(score, 100);
        }).orElse(0);
    }

    private int computeLeetGitScore(User user) {
        if (!user.isLeetgitEnabled()) return 0;
        long synced = leetGitSyncRepository.countSyncedByUserId(user.getId());
        return (int) Math.min(synced * 5, 100);
    }

    private int computeResumeScore(User user) {
        return resumeRepository.findByUserIdAndActiveTrue(user.getId())
                .map(r -> r.getAtsScore() != null ? r.getAtsScore() : 0)
                .orElse(0);
    }

    private int computeConsistencyScore(User user) {
        int streak = user.getCurrentStreak() != null ? user.getCurrentStreak() : 0;
        return Math.min(streak * 3, 100);
    }

    // ===== SUMMARY BUILDERS =====

    private String buildGitHubSummary(User user) {
        if (!user.isGithubConnected()) return "GitHub: Not connected";
        return gitHubProfileRepository.findByUserId(user.getId())
                .map(p -> String.format("GitHub: %d repos, %d stars, %d followers, %d streak days",
                        p.getPublicRepos() != null ? p.getPublicRepos() : 0,
                        p.getTotalStars() != null ? p.getTotalStars() : 0,
                        p.getFollowers() != null ? p.getFollowers() : 0,
                        p.getCurrentStreak() != null ? p.getCurrentStreak() : 0))
                .orElse("GitHub: Connected but no data yet");
    }

    private String buildLeetCodeSummary(User user) {
        if (!user.isLeetcodeConnected()) return "LeetCode: Not connected";
        return leetCodeProfileRepository.findByUserId(user.getId())
                .map(p -> String.format("LeetCode: %d total solved (Easy: %d, Medium: %d, Hard: %d), streak: %d",
                        p.getTotalSolved() != null ? p.getTotalSolved() : 0,
                        p.getEasySolved() != null ? p.getEasySolved() : 0,
                        p.getMediumSolved() != null ? p.getMediumSolved() : 0,
                        p.getHardSolved() != null ? p.getHardSolved() : 0,
                        p.getCurrentStreak() != null ? p.getCurrentStreak() : 0))
                .orElse("LeetCode: Connected but no data yet");
    }

    private String buildResumeSummary(User user) {
        return resumeRepository.findByUserIdAndActiveTrue(user.getId())
                .map(r -> String.format("Resume: ATS score %d/100, %d skills extracted",
                        r.getAtsScore() != null ? r.getAtsScore() : 0,
                        r.getExtractedSkillsJson() != null ? fromJsonList(r.getExtractedSkillsJson()).size() : 0))
                .orElse("Resume: Not uploaded");
    }

    // ===== HELPERS =====

    private DevDnaDto.DevDnaReportResponse toResponse(DevDnaReport r) {
        return DevDnaDto.DevDnaReportResponse.builder()
                .devDnaScore(r.getDevDnaScore())
                .interviewReadinessScore(r.getInterviewReadinessScore())
                .scoreBreakdown(DevDnaDto.ScoreBreakdown.builder()
                        .github(r.getGithubScore())
                        .leetcode(r.getLeetcodeScore())
                        .leetgit(r.getLeetgitScore())
                        .resume(r.getResumeScore())
                        .consistency(r.getConsistencyScore())
                        .aiAssessment(r.getAiAssessmentScore())
                        .build())
                .skillGraph(fromJsonMapList(r.getSkillGraphJson()))
                .strengths(fromJsonList(r.getStrengthsJson()))
                .areasToImprove(fromJsonList(r.getAreasToImproveJson()))
                .profileSummary(r.getProfileSummary())
                .motivationalMessage(r.getMotivationalMessage())
                .generatedAt(r.getGeneratedAt())
                .build();
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); } catch (Exception e) { return "[]"; }
    }

    private String toString(Object obj) { return obj != null ? obj.toString() : null; }

    private Integer toInt(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Integer i) return i;
        if (obj instanceof Number n) return n.intValue();
        try { return Integer.parseInt(obj.toString()); } catch (NumberFormatException e) { return 0; }
    }

    private List<String> fromJsonList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (Exception e) { return List.of(); }
    }

    private List<Map<String, Object>> fromJsonMapList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (Exception e) { return List.of(); }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> safeGetMap(Map<String, Object> data, String key) {
        Object val = data.get(key);
        return val instanceof Map ? (Map<String, Object>) val : null;
    }
}