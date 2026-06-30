package com.cultivationx.nexus.service;

import com.cultivationx.auth.entity.User;
import com.cultivationx.auth.repository.UserRepository;
import com.cultivationx.common.exception.AppException;
import com.cultivationx.nexus.dto.NexusDto;
import com.cultivationx.nexus.entity.GitHubProfile;
import com.cultivationx.nexus.repository.GitHubProfileRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubService {

    private final GitHubProfileRepository githubProfileRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.github.api-base-url:https://api.github.com}")
    private String githubApiUrl;

    @Value("${app.github.oauth-client-id:}")
    private String clientId;

    @Value("${app.github.oauth-client-secret:}")
    private String clientSecret;

    @Transactional
    public NexusDto.GitHubProfileResponse connectWithOAuth(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.userNotFound(email));

        // Exchange code for access token
        String accessToken = exchangeCodeForToken(code);

        // Fetch GitHub user profile
        Map<String, Object> githubUser = fetchGitHubUser(accessToken);

        String username = toString(githubUser.get("login"));

        // Update user entity
        user.setGithubUsername(username);
        user.setGithubAccessToken(accessToken);
        user.setGithubConnected(true);
        user.setGithubConnectedAt(LocalDateTime.now());
        userRepository.save(user);

        // Fetch and store full profile
        return syncProfile(user, accessToken, githubUser);
    }

    @Transactional
    public NexusDto.GitHubProfileResponse syncGitHub(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.userNotFound(email));

        if (!user.isGithubConnected()) {
            throw AppException.githubNotConnected();
        }

        Map<String, Object> githubUser = fetchGitHubUser(user.getGithubAccessToken());
        return syncProfile(user, user.getGithubAccessToken(), githubUser);
    }

    public NexusDto.GitHubProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.userNotFound(email));

        if (!user.isGithubConnected()) {
            return NexusDto.GitHubProfileResponse.builder().connected(false).build();
        }

        return githubProfileRepository.findByUserId(user.getId())
                .map(this::toResponse)
                .orElse(NexusDto.GitHubProfileResponse.builder().connected(false).build());
    }

    @Transactional
    public void disconnect(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.userNotFound(email));

        user.setGithubConnected(false);
        user.setGithubAccessToken(null);
        user.setGithubUsername(null);
        userRepository.save(user);

        githubProfileRepository.findByUserId(user.getId())
                .ifPresent(githubProfileRepository::delete);

        log.info("GitHub disconnected for user: {}", email);
    }

    // Sync for scheduler
    @Transactional
    public void syncAllConnectedUsers() {
        List<User> connectedUsers = userRepository.findAll().stream()
                .filter(User::isGithubConnected)
                .toList();

        for (User user : connectedUsers) {
            try {
                Map<String, Object> githubUser = fetchGitHubUser(user.getGithubAccessToken());
                syncProfile(user, user.getGithubAccessToken(), githubUser);
                log.info("GitHub synced for user: {}", user.getEmail());
            } catch (Exception e) {
                log.error("GitHub sync failed for user {}: {}", user.getEmail(), e.getMessage());
            }
        }
    }

    // ===== PRIVATE =====

    private NexusDto.GitHubProfileResponse syncProfile(User user, String accessToken, Map<String, Object> githubUser) {
        String username = toString(githubUser.get("login"));

        // Fetch repositories
        List<Map<String, Object>> repos = fetchRepositories(accessToken);
        int totalStars = repos.stream().mapToInt(r -> toInt(r.get("stargazers_count"))).sum();
        int totalForks = repos.stream().mapToInt(r -> toInt(r.get("forks_count"))).sum();

        // Top languages from repos
        Map<String, Integer> topLanguages = computeTopLanguages(repos);

        GitHubProfile profile = githubProfileRepository.findByUserId(user.getId())
                .orElse(GitHubProfile.builder().user(user).build());

        profile.setUsername(username);
        profile.setDisplayName(toString(githubUser.get("name")));
        profile.setAvatarUrl(toString(githubUser.get("avatar_url")));
        profile.setBio(toString(githubUser.get("bio")));
        profile.setCompany(toString(githubUser.get("company")));
        profile.setLocation(toString(githubUser.get("location")));
        profile.setBlog(toString(githubUser.get("blog")));
        profile.setPublicRepos(toInt(githubUser.get("public_repos")));
        profile.setFollowers(toInt(githubUser.get("followers")));
        profile.setFollowing(toInt(githubUser.get("following")));
        profile.setTotalStars(totalStars);
        profile.setTotalForks(totalForks);
        profile.setTopLanguagesJson(toJson(topLanguages));
        profile.setRepositoriesJson(toJson(repos.stream().limit(10).toList()));
        profile.setLastSyncAt(LocalDateTime.now());

        githubProfileRepository.save(profile);

        user.setGithubLastSyncAt(LocalDateTime.now());
        userRepository.save(user);

        return toResponse(profile);
    }

    private String exchangeCodeForToken(String code) {
        try {
            WebClient client = WebClient.builder()
                    .baseUrl("https://github.com")
                    .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            Map<String, Object> response = client.post()
                    .uri("/login/oauth/access_token")
                    .bodyValue(Map.of(
                            "client_id", clientId,
                            "client_secret", clientSecret,
                            "code", code
                    ))
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (response == null || !response.containsKey("access_token")) {
                throw AppException.githubApiError("Failed to obtain access token");
            }
            return toString(response.get("access_token"));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw AppException.githubApiError("OAuth exchange failed: " + e.getMessage());
        }
    }

    private Map<String, Object> fetchGitHubUser(String accessToken) {
        try {
            WebClient client = buildGitHubClient(accessToken);
            return client.get()
                    .uri("/user")
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (Exception e) {
            throw AppException.githubApiError("Failed to fetch user: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchRepositories(String accessToken) {
        try {
            WebClient client = buildGitHubClient(accessToken);
            List<Map<String, Object>> repos = client.get()
                    .uri("/user/repos?sort=updated&per_page=100&type=owner")
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .block();
            return repos != null ? repos : List.of();
        } catch (Exception e) {
            log.warn("Failed to fetch repositories: {}", e.getMessage());
            return List.of();
        }
    }

    private Map<String, Integer> computeTopLanguages(List<Map<String, Object>> repos) {
        Map<String, Integer> languages = new java.util.HashMap<>();
        for (Map<String, Object> repo : repos) {
            String lang = toString(repo.get("language"));
            if (lang != null && !lang.isBlank()) {
                languages.merge(lang, 1, Integer::sum);
            }
        }
        return languages;
    }

    private WebClient buildGitHubClient(String accessToken) {
        return WebClient.builder()
                .baseUrl(githubApiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
                .build();
    }

    @SuppressWarnings("unchecked")
    private NexusDto.GitHubProfileResponse toResponse(GitHubProfile p) {
        return NexusDto.GitHubProfileResponse.builder()
                .username(p.getUsername())
                .displayName(p.getDisplayName())
                .avatarUrl(p.getAvatarUrl())
                .bio(p.getBio())
                .company(p.getCompany())
                .location(p.getLocation())
                .publicRepos(p.getPublicRepos())
                .followers(p.getFollowers())
                .following(p.getFollowing())
                .totalStars(p.getTotalStars())
                .totalForks(p.getTotalForks())
                .currentStreak(p.getCurrentStreak())
                .longestStreak(p.getLongestStreak())
                .topLanguages(fromJson(p.getTopLanguagesJson(), new TypeReference<Map<String, Integer>>() {}))
                .repositories(fromJson(p.getRepositoriesJson(), new TypeReference<List<Map<String, Object>>>() {}))
                .lastSyncAt(p.getLastSyncAt())
                .connected(true)
                .build();
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); } catch (Exception e) { return "{}"; }
    }

    private <T> T fromJson(String json, TypeReference<T> type) {
        if (json == null) return null;
        try { return objectMapper.readValue(json, type); } catch (Exception e) { return null; }
    }

    private String toString(Object obj) { return obj != null ? obj.toString() : null; }

    private Integer toInt(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Integer i) return i;
        if (obj instanceof Number n) return n.intValue();
        try { return Integer.parseInt(obj.toString()); } catch (NumberFormatException e) { return 0; }
    }
}