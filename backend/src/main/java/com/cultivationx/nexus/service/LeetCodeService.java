package com.cultivationx.nexus.service;

import com.cultivationx.auth.entity.User;
import com.cultivationx.auth.repository.UserRepository;
import com.cultivationx.common.exception.AppException;
import com.cultivationx.nexus.dto.NexusDto;
import com.cultivationx.nexus.entity.LeetCodeProfile;
import com.cultivationx.nexus.repository.LeetCodeProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeetCodeService {

    private final LeetCodeProfileRepository leetCodeProfileRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // ===== PUBLIC METHODS =====

    @Transactional
    public NexusDto.LeetCodeProfileResponse connect(String email, String username) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.userNotFound(email));

        Map<String, Object> profileData = fetchLeetCodeProfile(username);
        if (profileData == null) {
            throw AppException.leetcodeApiError("Username not found or LeetCode API unavailable: " + username);
        }

        user.setLeetcodeUsername(username);
        user.setLeetcodeConnected(true);
        user.setLeetcodeConnectedAt(LocalDateTime.now());
        userRepository.save(user);

        return syncProfile(user, username, profileData);
    }

    @Transactional
    public NexusDto.LeetCodeProfileResponse sync(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.userNotFound(email));

        if (!user.isLeetcodeConnected()) {
            throw AppException.leetcodeNotConnected();
        }

        Map<String, Object> profileData = fetchLeetCodeProfile(user.getLeetcodeUsername());
        if (profileData == null) {
            throw AppException.leetcodeApiError("Failed to fetch data for: " + user.getLeetcodeUsername());
        }

        return syncProfile(user, user.getLeetcodeUsername(), profileData);
    }

    public NexusDto.LeetCodeProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.userNotFound(email));

        if (!user.isLeetcodeConnected()) {
            return NexusDto.LeetCodeProfileResponse.builder().connected(false).build();
        }

        return leetCodeProfileRepository.findByUserId(user.getId())
                .map(this::toResponse)
                .orElse(NexusDto.LeetCodeProfileResponse.builder().connected(false).build());
    }

    @Transactional
    public void disconnect(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.userNotFound(email));

        user.setLeetcodeConnected(false);
        user.setLeetcodeUsername(null);
        userRepository.save(user);

        leetCodeProfileRepository.findByUserId(user.getId())
                .ifPresent(leetCodeProfileRepository::delete);
    }

    @Transactional
    public void syncAllConnectedUsers() {
        List<User> connectedUsers = userRepository.findAll().stream()
                .filter(User::isLeetcodeConnected)
                .toList();

        for (User user : connectedUsers) {
            try {
                Map<String, Object> profileData = fetchLeetCodeProfile(user.getLeetcodeUsername());
                if (profileData != null) {
                    syncProfile(user, user.getLeetcodeUsername(), profileData);
                }
            } catch (Exception e) {
                log.error("LeetCode sync failed for {}: {}", user.getEmail(), e.getMessage());
            }
        }
    }

    // ===== PRIVATE =====

    /**
     * Fetches LeetCode stats using the public leetcode-stats-api proxy.
     * LeetCode's own GraphQL blocks direct server-to-server calls without a session cookie,
     * so we use this community proxy which handles auth internally.
     *
     * Returns data shaped as:
     * {
     *   userProfile:       { realName, userAvatar, ranking }
     *   submitStatsGlobal: { acSubmissionNum: [{ difficulty, count }] }
     *   streak:            { currentStreak, longestStreak, totalActiveDays }
     * }
     */
    private Map<String, Object> fetchLeetCodeProfile(String username) {
        try {
            WebClient client = WebClient.builder()
                    .baseUrl("https://alfa-leetcode-api.onrender.com")
                    .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = client.get()
                    .uri("/userProfile/" + username)
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            // API returns { status: "error" } if username not found
            if (response == null || "error".equals(response.get("status"))) {
                log.warn("LeetCode: username not found or API error for '{}'", username);
                return null;
            }

            // Map flat API response → nested structure that syncProfile expects
            List<Map<String, Object>> acSubmissionNum = List.of(
                    Map.of("difficulty", "All",    "count", response.getOrDefault("totalSolved",  0)),
                    Map.of("difficulty", "Easy",   "count", response.getOrDefault("easySolved",   0)),
                    Map.of("difficulty", "Medium", "count", response.getOrDefault("mediumSolved", 0)),
                    Map.of("difficulty", "Hard",   "count", response.getOrDefault("hardSolved",   0))
            );

            Map<String, Object> userProfile = new LinkedHashMap<>();
            userProfile.put("realName",   username); // stats API doesn't expose real name
            userProfile.put("userAvatar", "");
            userProfile.put("ranking",    response.getOrDefault("ranking", 0));

            Map<String, Object> submitStatsGlobal = new LinkedHashMap<>();
            submitStatsGlobal.put("acSubmissionNum", acSubmissionNum);

            Map<String, Object> streak = new LinkedHashMap<>();
            streak.put("currentStreak",   response.getOrDefault("totalActiveDays", 0)); // proxy doesn't expose streak separately
            streak.put("longestStreak",   0);
            streak.put("totalActiveDays", response.getOrDefault("totalActiveDays", 0));

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("userProfile",       userProfile);
            result.put("submitStatsGlobal", submitStatsGlobal);
            result.put("streak",            streak);

            return result;

        } catch (Exception e) {
            log.warn("LeetCode API call failed for '{}': {}", username, e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private NexusDto.LeetCodeProfileResponse syncProfile(User user, String username, Map<String, Object> data) {
        LeetCodeProfile profile = leetCodeProfileRepository.findByUserId(user.getId())
                .orElse(LeetCodeProfile.builder().user(user).build());

        profile.setUsername(username);

        // Parse userProfile
        Map<String, Object> userProfile = safeGetMap(data, "userProfile");
        if (userProfile != null) {
            profile.setRealName(toString(userProfile.get("realName")));
            profile.setAvatarUrl(toString(userProfile.get("userAvatar")));
            Object ranking = userProfile.get("ranking");
            profile.setRanking(ranking != null ? ranking.toString() : null);
        }

        // Parse submitStatsGlobal → acSubmissionNum
        Map<String, Object> submitStats = safeGetMap(data, "submitStatsGlobal");
        if (submitStats != null) {
            List<Map<String, Object>> acSubmissions =
                    (List<Map<String, Object>>) submitStats.get("acSubmissionNum");
            if (acSubmissions != null) {
                int total = 0, easy = 0, medium = 0, hard = 0;
                for (Map<String, Object> entry : acSubmissions) {
                    String difficulty = toString(entry.get("difficulty"));
                    int count = toInt(entry.get("count"));
                    if      ("All".equals(difficulty))    total  = count;
                    else if ("Easy".equals(difficulty))   easy   = count;
                    else if ("Medium".equals(difficulty)) medium = count;
                    else if ("Hard".equals(difficulty))   hard   = count;
                }
                profile.setTotalSolved(total);
                profile.setEasySolved(easy);
                profile.setMediumSolved(medium);
                profile.setHardSolved(hard);
            }
        }

        // Parse streak
        Map<String, Object> streakData = safeGetMap(data, "streak");
        if (streakData != null) {
            profile.setCurrentStreak(toInt(streakData.get("currentStreak")));
            profile.setLongestStreak(toInt(streakData.get("longestStreak")));
            profile.setTotalActiveDays(toInt(streakData.get("totalActiveDays")));
        }

        profile.setLastSyncAt(LocalDateTime.now());
        leetCodeProfileRepository.save(profile);

        user.setLeetcodeLastSyncAt(LocalDateTime.now());
        userRepository.save(user);

        return toResponse(profile);
    }

    private NexusDto.LeetCodeProfileResponse toResponse(LeetCodeProfile p) {
        return NexusDto.LeetCodeProfileResponse.builder()
                .username(p.getUsername())
                .realName(p.getRealName())
                .avatarUrl(p.getAvatarUrl())
                .ranking(p.getRanking())
                .totalSolved(p.getTotalSolved()      != null ? p.getTotalSolved()      : 0)
                .easySolved(p.getEasySolved()        != null ? p.getEasySolved()        : 0)
                .mediumSolved(p.getMediumSolved()    != null ? p.getMediumSolved()      : 0)
                .hardSolved(p.getHardSolved()        != null ? p.getHardSolved()        : 0)
                .acceptanceRate(p.getAcceptanceRate())
                .contestRating(p.getContestRating())
                .currentStreak(p.getCurrentStreak()  != null ? p.getCurrentStreak()    : 0)
                .longestStreak(p.getLongestStreak()  != null ? p.getLongestStreak()     : 0)
                .totalActiveDays(p.getTotalActiveDays() != null ? p.getTotalActiveDays() : 0)
                .lastSyncAt(p.getLastSyncAt())
                .connected(true)
                .build();
    }

    // ===== HELPERS =====

    @SuppressWarnings("unchecked")
    private Map<String, Object> safeGetMap(Map<String, Object> data, String key) {
        Object val = data.get(key);
        return val instanceof Map ? (Map<String, Object>) val : null;
    }

    private String toString(Object obj) {
        return obj != null ? obj.toString() : null;
    }

    private Integer toInt(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Integer i) return i;
        if (obj instanceof Number n) return n.intValue();
        try { return Integer.parseInt(obj.toString()); }
        catch (NumberFormatException e) { return 0; }
    }
}