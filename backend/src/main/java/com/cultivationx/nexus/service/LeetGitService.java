package com.cultivationx.nexus.service;

import com.cultivationx.ai.service.AiService;
import com.cultivationx.auth.entity.User;
import com.cultivationx.auth.repository.UserRepository;
import com.cultivationx.common.exception.AppException;
import com.cultivationx.nexus.dto.NexusDto;
import com.cultivationx.nexus.entity.LeetGitSync;
import com.cultivationx.nexus.repository.LeetGitSyncRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeetGitService {

    private final LeetGitSyncRepository leetGitSyncRepository;
    private final UserRepository userRepository;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    @Value("${app.github.api-base-url:https://api.github.com}")
    private String githubApiUrl;

    @Transactional
    public NexusDto.LeetGitSyncResponse syncSolution(String email, NexusDto.LeetGitSyncRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.userNotFound(email));

        if (!user.isGithubConnected()) throw AppException.githubNotConnected();
        if (!user.isLeetgitEnabled()) throw AppException.badRequest("LeetGit not enabled. Please enable it first.");

        // Check if already synced
        LeetGitSync existing = leetGitSyncRepository
                .findByUserIdAndProblemSlug(user.getId(), request.getProblemSlug())
                .orElse(null);

        LeetGitSync sync = existing != null ? existing : LeetGitSync.builder().user(user).build();

        sync.setProblemTitle(request.getProblemTitle());
        sync.setProblemSlug(request.getProblemSlug());
        sync.setLanguage(request.getLanguage());
        sync.setDifficulty(request.getDifficulty() != null ? request.getDifficulty() : "Medium");
        sync.setUserCode(request.getCode());
        sync.setStatus(LeetGitSync.SyncStatus.AI_REVIEWING);
        sync = leetGitSyncRepository.save(sync);

        // AI Review
        Map<String, Object> aiResult = aiService.generateLeetHubReadme(
                request.getProblemTitle(),
                request.getProblemDescription() != null ? request.getProblemDescription() : request.getProblemTitle(),
                request.getCode(),
                request.getLanguage(),
                sync.getDifficulty()
        );

        sync.setAiReviewJson(toJson(aiResult.get("codeReview")));
        sync.setOptimizedCode(toString(aiResult.get("optimizedCode")));
        sync.setInterviewNotes(toJson(aiResult.get("interviewNotes")));
        sync.setTimeComplexity(toString(aiResult.get("timeComplexity")));
        sync.setSpaceComplexity(toString(aiResult.get("spaceComplexity")));
        sync.setAiScore(toInt(((Map<?, ?>) aiResult.getOrDefault("codeReview", Map.of())).get("score")));

        // Generate README
        String readme = buildReadme(request, aiResult);
        sync.setReadmeContent(readme);

        // Push to GitHub
        sync.setStatus(LeetGitSync.SyncStatus.PUSHING);
        sync = leetGitSyncRepository.save(sync);

        try {
            String commitUrl = pushToGitHub(user, sync, request, readme);
            sync.setGithubCommitUrl(commitUrl);
            sync.setStatus(LeetGitSync.SyncStatus.SYNCED);
            sync.setSyncedAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("GitHub push failed: {}", e.getMessage());
            sync.setStatus(LeetGitSync.SyncStatus.FAILED);
        }

        sync = leetGitSyncRepository.save(sync);
        return toResponse(sync, aiResult);
    }

    @Transactional
    public void enableLeetGit(String email, String repoName) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.userNotFound(email));

        if (!user.isGithubConnected()) throw AppException.githubNotConnected();

        // Create repository if it doesn't exist
        try {
            createGitHubRepo(user, repoName);
        } catch (Exception e) {
            log.warn("Repo may already exist: {}", e.getMessage());
        }

        user.setLeetgitRepoName(repoName);
        user.setLeetgitEnabled(true);
        userRepository.save(user);
    }

    public NexusDto.LeetGitStats getStats(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.userNotFound(email));
        if (!user.isLeetgitEnabled()) {
            return null;
        }

        long total = leetGitSyncRepository.countTotalByUserId(user.getId());
        long synced = leetGitSyncRepository.countSyncedByUserId(user.getId());
        double successRate = total > 0 ? (double) synced / total * 100 : 0;

        List<NexusDto.LeetGitSyncResponse> recent = leetGitSyncRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, 10))
                .stream()
                .map(s -> toResponse(s, null))
                .toList();

        return NexusDto.LeetGitStats.builder()
                .totalSynced(total)
                .totalPushed(synced)
                .syncSuccessRate(Math.round(successRate * 10.0) / 10.0)
                .lastSyncAt(user.getLeetgitLastSyncAt())
                .recentSyncs(recent)
                .build();
    }

    public List<NexusDto.LeetGitSyncResponse> getHistory(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.userNotFound(email));

        return leetGitSyncRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, 50))
                .stream()
                .map(s -> toResponse(s, null))
                .toList();
    }

    // ===== PRIVATE =====

    private String buildReadme(NexusDto.LeetGitSyncRequest request, Map<String, Object> aiResult) {
        String approach = toString(aiResult.get("approach"));
        String timeComplexity = toString(aiResult.get("timeComplexity"));
        String spaceComplexity = toString(aiResult.get("spaceComplexity"));
        String optimizedCode = toString(aiResult.get("optimizedCode"));

        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(request.getProblemTitle()).append("\n\n");
        sb.append("**Difficulty:** ").append(request.getDifficulty()).append("\n\n");
        sb.append("## Problem Summary\n").append(toString(aiResult.get("problemSummary"))).append("\n\n");
        sb.append("## My Approach\n").append(approach != null ? approach : "See code below").append("\n\n");
        sb.append("## Complexity Analysis\n");
        sb.append("- **Time:** ").append(timeComplexity).append("\n");
        sb.append("- **Space:** ").append(spaceComplexity).append("\n\n");
        sb.append("## My Solution\n```").append(request.getLanguage().toLowerCase()).append("\n");
        sb.append(request.getCode()).append("\n```\n\n");
        if (optimizedCode != null && !optimizedCode.isBlank()) {
            sb.append("## AI Optimized Solution\n```").append(request.getLanguage().toLowerCase()).append("\n");
            sb.append(optimizedCode).append("\n```\n\n");
        }
        sb.append("## Interview Notes\n");
        Object notes = aiResult.get("interviewNotes");
        if (notes instanceof List<?> notesList) {
            notesList.forEach(n -> sb.append("- ").append(n).append("\n"));
        }
        sb.append("\n## Related Problems\n");
        Object related = aiResult.get("relatedProblems");
        if (related instanceof List<?> relatedList) {
            relatedList.forEach(r -> sb.append("- ").append(r).append("\n"));
        }
        return sb.toString();
    }

    private String pushToGitHub(User user, LeetGitSync sync,
                                NexusDto.LeetGitSyncRequest request, String readmeContent) {
        WebClient client = WebClient.builder()
                .baseUrl(githubApiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + user.getGithubAccessToken())
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
                .build();

        String repoName = user.getLeetgitRepoName();
        String owner = user.getGithubUsername();
        String folderPath = formatProblemPath(request.getProblemTitle(), request.getProblemSlug());
        String fileExt = getFileExtension(request.getLanguage());

        // Push README
        pushFile(client, owner, repoName,
                folderPath + "/README.md",
                readmeContent,
                "Add " + request.getProblemTitle() + " README with AI review");

        // Push user's original solution
        pushFile(client, owner, repoName,
                folderPath + "/Solution." + fileExt,
                request.getCode(),
                "Add " + request.getProblemTitle() + " user solution");

        // Push AI optimized solution (if available)
        String optimizedCode = sync.getOptimizedCode();
        if (optimizedCode != null && !optimizedCode.isBlank()) {
            pushFile(client, owner, repoName,
                    folderPath + "/Solution_AI." + fileExt,
                    optimizedCode,
                    "Add " + request.getProblemTitle() + " AI optimized solution");
        }

        return "https://github.com/" + owner + "/" + repoName + "/tree/main/" + folderPath;
    }

    private void pushFile(WebClient client, String owner, String repo,
                          String path, String content, String message) {
        String encodedContent = Base64.getEncoder().encodeToString(content.getBytes());

        // Check if file exists (to get sha for update)
        String sha = null;
        try {
            Map<String, Object> existing = client.get()
                    .uri("/repos/{owner}/{repo}/contents/{path}", owner, repo, path)
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            if (existing != null) sha = toString(existing.get("sha"));
        } catch (Exception ignored) {}

        Map<String, Object> body = sha != null
                ? Map.of("message", message, "content", encodedContent, "sha", sha)
                : Map.of("message", message, "content", encodedContent);

        client.put()
                .uri("/repos/{owner}/{repo}/contents/{path}", owner, repo, path)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    private void createGitHubRepo(User user, String repoName) {
        WebClient client = WebClient.builder()
                .baseUrl(githubApiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + user.getGithubAccessToken())
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
                .build();

        client.post()
                .uri("/user/repos")
                .bodyValue(Map.of(
                        "name", repoName,
                        "description", "LeetCode solutions with AI reviews — powered by CultivationX LeetGit",
                        "private", false,
                        "auto_init", true
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    private String formatProblemPath(String title, String slug) {
        return slug.replaceAll("[^a-zA-Z0-9-]", "-");
    }

    private String getFileExtension(String language) {
        return switch (language.toLowerCase()) {
            case "java" -> "java";
            case "python", "python3" -> "py";
            case "javascript" -> "js";
            case "typescript" -> "ts";
            case "cpp", "c++" -> "cpp";
            case "c" -> "c";
            case "go" -> "go";
            case "rust" -> "rs";
            case "kotlin" -> "kt";
            default -> "txt";
        };
    }

    @SuppressWarnings("unchecked")
    private NexusDto.LeetGitSyncResponse toResponse(LeetGitSync s, Map<String, Object> aiResult) {
        Map<String, Object> review = null;
        if (s.getAiReviewJson() != null) {
            try { review = objectMapper.readValue(s.getAiReviewJson(), new TypeReference<>() {}); }
            catch (Exception ignored) {}
        }

        return NexusDto.LeetGitSyncResponse.builder()
                .id(s.getId())
                .problemTitle(s.getProblemTitle())
                .problemSlug(s.getProblemSlug())
                .difficulty(s.getDifficulty())
                .language(s.getLanguage())
                .aiScore(s.getAiScore())
                .timeComplexity(s.getTimeComplexity())
                .spaceComplexity(s.getSpaceComplexity())
                .aiReview(review)
                .optimizedCode(s.getOptimizedCode())
                .githubCommitUrl(s.getGithubCommitUrl())
                .status(s.getStatus())
                .createdAt(s.getCreatedAt())
                .syncedAt(s.getSyncedAt())
                .build();
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); } catch (Exception e) { return "{}"; }
    }

    private String toString(Object obj) { return obj != null ? obj.toString() : null; }

    private Integer toInt(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Integer i) return i;
        if (obj instanceof Number n) return n.intValue();
        try { return Integer.parseInt(obj.toString()); } catch (NumberFormatException e) { return 0; }
    }
}