package com.cultivationx.ai.service;

import com.cultivationx.common.exception.AppException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    // ===== RISE: ATS Analysis =====
    public Map<String, Object> analyzeResume(String resumeText, String goal) {
        String prompt = """
            You are an expert ATS (Applicant Tracking System) and resume analyzer.
            
            Analyze the following resume for a developer targeting: %s
            
            Resume Content:
            %s
            
            Return ONLY a valid JSON object (no markdown, no explanation) with this exact structure:
            {
              "atsScore": <integer 0-100>,
              "extractedSkills": ["skill1", "skill2", ...],
              "experienceSummary": "<brief summary>",
              "educationSummary": "<brief summary>",
              "projectsSummary": "<brief summary>",
              "missingKeywords": ["keyword1", "keyword2", ...],
              "strengths": ["strength1", ...],
              "improvements": ["suggestion1", "suggestion2", ...],
              "keywordMatchScore": <integer 0-100>,
              "formattingScore": <integer 0-100>,
              "actionVerbScore": <integer 0-100>,
              "quantifiedAchievementsScore": <integer 0-100>,
              "overallFeedback": "<2-3 sentence summary>"
            }
            """.formatted(goal, resumeText);

        return callAiForJson(prompt);
    }

    // ===== RISE: Skill Gap Analysis =====
    public Map<String, Object> analyzeSkillGap(String resumeText, String goal, String githubSkills, String leetcodeStats) {
        String prompt = """
            You are an expert developer career coach analyzing skill gaps.
            
            Developer Goal: %s
            Resume Skills: %s
            GitHub Activity (languages/projects): %s
            LeetCode Stats: %s
            
            Analyze the gaps and create a personalized 8-week learning roadmap.
            Return ONLY valid JSON with this structure:
            {
              "existingSkills": [{"skill": "Java", "proficiency": 85, "source": "resume+github"}],
              "missingSkills": [{"skill": "Docker", "priority": "high", "reason": "Required for backend roles"}],
              "weeklyRoadmap": [
                {"week": 1, "focus": "Topic", "tasks": ["task1", "task2"], "resources": ["resource1"]}
              ],
              "priorityAreas": ["area1", "area2"],
              "estimatedReadinessWeeks": <integer>,
              "currentReadinessPercent": <integer 0-100>
            }
            """.formatted(goal, resumeText, githubSkills, leetcodeStats);

        return callAiForJson(prompt);
    }

    // ===== RISE: Learning Path =====
    public Map<String, Object> generateLearningPath(String goal, List<String> missingSkills, String currentLevel) {
        String prompt = """
            You are a senior developer career mentor.
            
            Developer Goal: %s
            Current Level: %s
            Skills to Develop: %s
            
            Create a detailed, personalized learning path. Return ONLY valid JSON:
            {
              "totalWeeks": <integer>,
              "phases": [
                {
                  "phase": 1,
                  "title": "Phase name",
                  "weeks": "1-2",
                  "skills": ["skill1", "skill2"],
                  "dailyTasks": ["task1", "task2"],
                  "projects": ["project idea"],
                  "resources": [{"name": "Resource", "url": "url", "type": "video|article|book"}],
                  "milestone": "What you'll be able to do"
                }
              ],
              "dailyHoursRequired": <integer>,
              "weeklyGoals": ["goal1", "goal2"]
            }
            """.formatted(goal, currentLevel, String.join(", ", missingSkills));

        return callAiForJson(prompt);
    }

    // ===== MENTOR: AI Chat =====
    public String chat(String userMessage, List<Map<String, String>> conversationHistory, String userContext) {
        StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append("""
            You are an expert AI coding mentor and career coach for software developers.
            You help with: Data Structures & Algorithms, System Design, Java/Spring Boot,
            React, coding best practices, career advice, and interview preparation.
            
            Be concise, practical, and use code examples when relevant.
            Format code in markdown code blocks with language specified.
            """);

        if (userContext != null && !userContext.isBlank()) {
            systemPrompt.append("\nUser Context: ").append(userContext);
        }

        try {
            var messages = new java.util.ArrayList<Message>();
            messages.add(new SystemMessage(systemPrompt.toString()));

            for (Map<String, String> msg : conversationHistory) {
                if ("user".equals(msg.get("role"))) {
                    messages.add(new UserMessage(msg.get("content")));
                } else {
                    messages.add(new AssistantMessage(msg.get("content")));
                }
            }
            messages.add(new UserMessage(userMessage));

            return chatClient.prompt()
                    .messages(messages)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("AI chat error: {}", e.getMessage());
            throw AppException.aiServiceError(e.getMessage());
        }
    }

    // ===== MENTOR: Code Review =====
    public Map<String, Object> reviewCode(String code, String language, String problemContext) {
        String prompt = """
            You are a senior software engineer conducting a thorough code review.
            
            Language: %s
            Problem/Context: %s
            
            Code to Review:
            ```%s
            %s
            ```
            
            Return ONLY valid JSON:
            {
              "overallScore": <integer 1-10>,
              "grade": "A|B|C|D|F",
              "correctness": {"score": <1-10>, "issues": ["issue1"]},
              "performance": {"score": <1-10>, "timeComplexity": "O(n)", "spaceComplexity": "O(1)", "issues": []},
              "security": {"score": <1-10>, "issues": []},
              "readability": {"score": <1-10>, "issues": []},
              "bestPractices": {"score": <1-10>, "issues": []},
              "positives": ["what's good"],
              "improvements": [{"line": <int or null>, "issue": "description", "fix": "how to fix"}],
              "interviewTips": ["tip1", "tip2"],
              "commonMistakes": ["mistake1"],
              "relatedConcepts": ["concept1"]
            }
            """.formatted(language, problemContext, language.toLowerCase(), code);

        return callAiForJson(prompt);
    }

    // ===== MENTOR: Better Solution =====
    public Map<String, Object> generateBetterSolution(String code, String language, String problemContext) {
        String prompt = """
            You are an expert competitive programmer and software engineer.
            
            Language: %s
            Problem: %s
            Current Solution:
            ```%s
            %s
            ```
            
            Provide an optimal solution. Return ONLY valid JSON:
            {
              "originalApproach": "Brief description of original approach",
              "originalComplexity": {"time": "O(n²)", "space": "O(1)"},
              "optimizedCode": "```%s\\n<complete optimized code>\\n```",
              "optimizedApproach": "Brief description",
              "optimizedComplexity": {"time": "O(n)", "space": "O(n)"},
              "tradeoffs": "When to use original vs optimized",
              "interviewExplanation": "How to explain this in an interview",
              "alternativeApproaches": [
                {"approach": "Approach name", "complexity": "O(n)", "whenToUse": "When"}
              ],
              "followUpQuestions": ["question1", "question2"]
            }
            """.formatted(language, problemContext, language.toLowerCase(), code, language.toLowerCase());

        return callAiForJson(prompt);
    }

    // ===== NEXUS: LeetHub README =====
    public Map<String, Object> generateLeetHubReadme(String problemTitle, String problemDescription,
                                                     String userCode, String language, String difficulty) {
        String prompt = """
            You are creating professional documentation for a LeetCode solution.
            
            Problem: %s (Difficulty: %s)
            Description: %s
            Language: %s
            User's Solution:
            ```%s
            %s
            ```
            
            Return ONLY valid JSON:
            {
              "problemSummary": "1-2 sentence explanation",
              "approach": "Detailed explanation of the approach",
              "timeComplexity": "O(?)",
              "spaceComplexity": "O(?)",
              "codeReview": {
                "score": <1-10>,
                "positives": ["what's good"],
                "improvements": ["improvement1"]
              },
              "optimizedCode": "complete optimized solution in %s",
              "optimizedApproach": "explanation of optimized approach",
              "optimizedTimeComplexity": "O(?)",
              "interviewNotes": ["note1", "note2"],
              "relatedProblems": ["problem1", "problem2"],
              "tags": ["tag1", "tag2"]
            }
            """.formatted(problemTitle, difficulty, problemDescription,
                language, language.toLowerCase(), userCode, language);

        return callAiForJson(prompt);
    }

    // ===== DEV DNA: Weekly Report =====
    public Map<String, Object> generateWeeklyReport(String userName, int devDnaScore,
                                                    String githubStats, String leetcodeStats,
                                                    String resumeStats, String goal) {
        String prompt = """
            You are an AI career coach generating a weekly developer progress report.
            
            Developer: %s
            Goal: %s
            Current Dev DNA Score: %d/100
            GitHub Stats: %s
            LeetCode Stats: %s
            Resume Stats: %s
            
            Return ONLY valid JSON:
            {
              "headline": "One powerful headline about their week",
              "overallProgress": "2-3 sentence summary",
              "strengths": ["strength1", "strength2"],
              "weaknesses": ["weakness1", "weakness2"],
              "insights": ["insight1", "insight2", "insight3"],
              "todaysMission": {
                "title": "Today's Mission",
                "tasks": [
                  {"task": "description", "impact": "+X Dev DNA", "priority": "high|medium|low"}
                ],
                "totalImpact": "+X Dev DNA points"
              },
              "weeklyGoals": [
                {"day": "Monday", "focus": "topic", "tasks": ["task1"]}
              ],
              "interviewReadiness": {
                "score": <integer 0-100>,
                "breakdown": {
                  "resume": <0-100>,
                  "github": <0-100>,
                  "leetcode": <0-100>,
                  "projects": <0-100>,
                  "consistency": <0-100>
                },
                "readyFor": ["Company type user is ready for"],
                "notReadyFor": ["Company type user isn't ready for yet"]
              },
              "motivationalMessage": "Personalized motivational message"
            }
            """.formatted(userName, goal, devDnaScore, githubStats, leetcodeStats, resumeStats);

        return callAiForJson(prompt);
    }

    // ===== DEV DNA: Full Analysis =====
    public Map<String, Object> generateDevDnaAnalysis(String userName, String goal,
                                                      String githubData, String leetcodeData,
                                                      String resumeData) {
        String prompt = """
            You are an AI developer intelligence engine performing a comprehensive developer analysis.
            
            Developer: %s | Goal: %s
            GitHub Data: %s
            LeetCode Data: %s
            Resume Data: %s
            
            Return ONLY valid JSON:
            {
              "devDnaScore": <integer 0-100>,
              "interviewReadinessScore": <integer 0-100>,
              "skillGraph": [
                {"skill": "Java", "score": 85, "category": "backend|frontend|dsa|devops|soft"}
              ],
              "topStrengths": ["strength1", "strength2", "strength3"],
              "areasToImprove": ["area1", "area2"],
              "profileSummary": "3-4 sentence AI-generated profile",
              "readyForRoles": ["Role 1", "Role 2"],
              "notReadyForRoles": ["Role that needs more prep"],
              "monthlyRoadmap": {
                "week1": {"focus": "Topic", "tasks": ["task1"]},
                "week2": {"focus": "Topic", "tasks": ["task1"]},
                "week3": {"focus": "Topic", "tasks": ["task1"]},
                "week4": {"focus": "Topic", "tasks": ["task1"]}
              }
            }
            """.formatted(userName, goal, githubData, leetcodeData, resumeData);

        return callAiForJson(prompt);
    }

    // ===== HELPER =====
    private Map<String, Object> callAiForJson(String prompt) {
        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            // Strip markdown code fences if present
            String cleaned = response.trim();
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.substring(7);
            } else if (cleaned.startsWith("```")) {
                cleaned = cleaned.substring(3);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
            cleaned = cleaned.trim();

            return objectMapper.readValue(cleaned, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("AI service error: {}", e.getMessage());
            throw AppException.aiServiceError(e.getMessage());
        }
    }

    public String callAiForText(String prompt) {
        try {
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("AI service error: {}", e.getMessage());
            throw AppException.aiServiceError(e.getMessage());
        }
    }
}