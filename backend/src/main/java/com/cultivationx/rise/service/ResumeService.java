package com.cultivationx.rise.service;

import com.cultivationx.ai.service.AiService;
import com.cultivationx.auth.entity.User;
import com.cultivationx.auth.repository.UserRepository;
import com.cultivationx.common.exception.AppException;
import com.cultivationx.rise.dto.RiseDto;
import com.cultivationx.rise.entity.Resume;
import com.cultivationx.rise.entity.SkillGap;
import com.cultivationx.rise.parser.ResumeParser;
import com.cultivationx.rise.repository.ResumeRepository;
import com.cultivationx.rise.repository.SkillGapRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final SkillGapRepository skillGapRepository;
    private final UserRepository userRepository;
    private final ResumeParser resumeParser;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    @Value("${app.resume.upload-dir:./uploads/resumes}")
    private String uploadDir;

    @Transactional
    public RiseDto.ResumeResponse uploadAndAnalyze(String email, MultipartFile file) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.userNotFound(email));

        // Validate file
        String fileType = resumeParser.validateAndDetectType(file);

        // Extract text
        String rawText = resumeParser.extractText(file);

        // Save file to disk
        String filePath = saveFileToDisk(file, user.getId());

        // Deactivate previous active resume
        resumeRepository.deactivateAllForUser(user.getId());

        // Get version number
        int version = resumeRepository.countByUserId(user.getId()) + 1;

        // Create resume record
        Resume resume = Resume.builder()
                .user(user)
                .fileName(file.getOriginalFilename())
                .fileType(fileType)
                .fileSize(file.getSize())
                .filePath(filePath)
                .rawText(rawText)
                .status(Resume.ResumeStatus.ANALYZING)
                .active(true)
                .version(version)
                .build();

        resume = resumeRepository.save(resume);

        // Run AI analysis
        try {
            String goalStr = user.getGoal() != null ? user.getGoal().name() : "SDE_1";
            Map<String, Object> analysis = aiService.analyzeResume(rawText, goalStr);
            applyAnalysisToResume(resume, analysis);
            resume.setStatus(Resume.ResumeStatus.ANALYZED);
            resume.setAnalyzedAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("AI analysis failed: {}", e.getMessage());
            resume.setStatus(Resume.ResumeStatus.FAILED);
        }

        resume = resumeRepository.save(resume);

        // Run skill gap analysis in background
        if (resume.getStatus() == Resume.ResumeStatus.ANALYZED) {
            generateSkillGap(user, resume);
        }

        return toResponse(resume);
    }

    @Transactional
    public RiseDto.ResumeResponse reanalyze(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.userNotFound(email));

        Resume resume = resumeRepository.findByUserIdAndActiveTrue(user.getId())
                .orElseThrow(() -> AppException.resumeNotFound(user.getId()));

        resume.setStatus(Resume.ResumeStatus.ANALYZING);
        resume = resumeRepository.save(resume);

        try {
            String goalStr = user.getGoal() != null ? user.getGoal().name() : "SDE_1";
            Map<String, Object> analysis = aiService.analyzeResume(resume.getRawText(), goalStr);
            applyAnalysisToResume(resume, analysis);
            resume.setStatus(Resume.ResumeStatus.ANALYZED);
            resume.setAnalyzedAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Re-analysis failed: {}", e.getMessage());
            resume.setStatus(Resume.ResumeStatus.FAILED);
        }

        return toResponse(resumeRepository.save(resume));
    }

    public RiseDto.ResumeResponse getActiveResume(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.userNotFound(email));

        Resume resume = resumeRepository.findByUserIdAndActiveTrue(user.getId())
                .orElseThrow(() -> AppException.resumeNotFound(user.getId()));

        return toResponse(resume);
    }

    public List<RiseDto.ResumeResponse> getHistory(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.userNotFound(email));

        return resumeRepository.findByUserIdOrderByUploadedAtDesc(user.getId())
                .stream().map(this::toResponse).toList();
    }

    public RiseDto.SkillGapResponse getSkillGap(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.userNotFound(email));

        SkillGap skillGap = skillGapRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId())
                .orElseThrow(() -> AppException.notFound("Skill gap analysis"));

        return toSkillGapResponse(skillGap);
    }

    public RiseDto.RiseDashboardResponse getDashboard(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.userNotFound(email));

        RiseDto.ResumeResponse activeResume = resumeRepository.findByUserIdAndActiveTrue(user.getId())
                .map(this::toResponse).orElse(null);

        RiseDto.SkillGapResponse skillGap = skillGapRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId())
                .map(this::toSkillGapResponse).orElse(null);

        int totalUploads = resumeRepository.countByUserId(user.getId());

        return RiseDto.RiseDashboardResponse.builder()
                .activeResume(activeResume)
                .latestSkillGap(skillGap)
                .totalResumesUploaded(totalUploads)
                .build();
    }

    // ===== PRIVATE HELPERS =====

    private void generateSkillGap(User user, Resume resume) {
        try {
            String goalStr = user.getGoal() != null ? user.getGoal().name() : "SDE_1";
            String githubSkills = user.isGithubConnected() ? "GitHub connected: " + user.getGithubUsername() : "Not connected";
            String leetcodeStats = user.isLeetcodeConnected() ? "LeetCode: " + user.getLeetcodeUsername() : "Not connected";

            Map<String, Object> gapData = aiService.analyzeSkillGap(
                    resume.getRawText(), goalStr, githubSkills, leetcodeStats);

            SkillGap skillGap = SkillGap.builder()
                    .user(user)
                    .existingSkillsJson(toJson(gapData.get("existingSkills")))
                    .missingSkillsJson(toJson(gapData.get("missingSkills")))
                    .weeklyRoadmapJson(toJson(gapData.get("weeklyRoadmap")))
                    .priorityAreasJson(toJson(gapData.get("priorityAreas")))
                    .estimatedReadinessWeeks(toInt(gapData.get("estimatedReadinessWeeks")))
                    .currentReadinessPercent(toInt(gapData.get("currentReadinessPercent")))
                    .build();

            skillGapRepository.save(skillGap);
        } catch (Exception e) {
            log.error("Skill gap generation failed: {}", e.getMessage());
        }
    }

    private void applyAnalysisToResume(Resume resume, Map<String, Object> analysis) {
        resume.setAtsScore(toInt(analysis.get("atsScore")));
        resume.setKeywordMatchScore(toInt(analysis.get("keywordMatchScore")));
        resume.setFormattingScore(toInt(analysis.get("formattingScore")));
        resume.setActionVerbScore(toInt(analysis.get("actionVerbScore")));
        resume.setQuantifiedAchievementsScore(toInt(analysis.get("quantifiedAchievementsScore")));
        resume.setExtractedSkillsJson(toJson(analysis.get("extractedSkills")));
        resume.setMissingKeywordsJson(toJson(analysis.get("missingKeywords")));
        resume.setImprovementsJson(toJson(analysis.get("improvements")));
        resume.setStrengthsJson(toJson(analysis.get("strengths")));
        resume.setExperienceSummary(toString(analysis.get("experienceSummary")));
        resume.setEducationSummary(toString(analysis.get("educationSummary")));
        resume.setProjectsSummary(toString(analysis.get("projectsSummary")));
        resume.setOverallFeedback(toString(analysis.get("overallFeedback")));
    }

    private String saveFileToDisk(MultipartFile file, Long userId) {
        try {
            Path dir = Paths.get(uploadDir, userId.toString());
            Files.createDirectories(dir);
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = dir.resolve(filename);
            file.transferTo(filePath);
            return filePath.toString();
        } catch (IOException e) {
            throw AppException.internalError("Failed to save file: " + e.getMessage());
        }
    }

    private RiseDto.ResumeResponse toResponse(Resume r) {
        return RiseDto.ResumeResponse.builder()
                .id(r.getId())
                .fileName(r.getFileName())
                .fileType(r.getFileType())
                .fileSize(r.getFileSize())
                .atsScore(r.getAtsScore())
                .keywordMatchScore(r.getKeywordMatchScore())
                .formattingScore(r.getFormattingScore())
                .actionVerbScore(r.getActionVerbScore())
                .quantifiedAchievementsScore(r.getQuantifiedAchievementsScore())
                .extractedSkills(fromJsonList(r.getExtractedSkillsJson()))
                .missingKeywords(fromJsonList(r.getMissingKeywordsJson()))
                .improvements(fromJsonList(r.getImprovementsJson()))
                .strengths(fromJsonList(r.getStrengthsJson()))
                .experienceSummary(r.getExperienceSummary())
                .educationSummary(r.getEducationSummary())
                .projectsSummary(r.getProjectsSummary())
                .overallFeedback(r.getOverallFeedback())
                .status(r.getStatus())
                .active(r.isActive())
                .version(r.getVersion())
                .uploadedAt(r.getUploadedAt())
                .analyzedAt(r.getAnalyzedAt())
                .build();
    }

    private RiseDto.SkillGapResponse toSkillGapResponse(SkillGap sg) {
        return RiseDto.SkillGapResponse.builder()
                .existingSkills(fromJsonMapList(sg.getExistingSkillsJson()))
                .missingSkills(fromJsonMapList(sg.getMissingSkillsJson()))
                .weeklyRoadmap(fromJsonMapList(sg.getWeeklyRoadmapJson()))
                .priorityAreas(fromJsonList(sg.getPriorityAreasJson()))
                .estimatedReadinessWeeks(sg.getEstimatedReadinessWeeks())
                .currentReadinessPercent(sg.getCurrentReadinessPercent())
                .createdAt(sg.getCreatedAt())
                .build();
    }

    private String toJson(Object obj) {
        if (obj == null) return "[]";
        try { return objectMapper.writeValueAsString(obj); }
        catch (JsonProcessingException e) { return "[]"; }
    }

    private String toString(Object obj) {
        return obj != null ? obj.toString() : null;
    }

    private Integer toInt(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Integer i) return i;
        if (obj instanceof Number n) return n.intValue();
        try { return Integer.parseInt(obj.toString()); }
        catch (NumberFormatException e) { return null; }
    }

    private List<String> fromJsonList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (Exception e) { return List.of(); }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fromJsonMapList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (Exception e) { return List.of(); }
    }
}