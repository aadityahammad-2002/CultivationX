package com.cultivationx.mentor.service;

import com.cultivationx.ai.service.AiService;
import com.cultivationx.auth.entity.User;
import com.cultivationx.auth.repository.UserRepository;
import com.cultivationx.common.exception.AppException;
import com.cultivationx.mentor.dto.MentorDto;
import com.cultivationx.mentor.entity.ChatConversation;
import com.cultivationx.mentor.entity.ChatMessage;
import com.cultivationx.mentor.entity.CodeReview;
import com.cultivationx.mentor.repository.ChatConversationRepository;
import com.cultivationx.mentor.repository.ChatMessageRepository;
import com.cultivationx.mentor.repository.CodeReviewRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MentorService {

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final CodeReviewRepository codeReviewRepository;
    private final UserRepository userRepository;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    @Transactional
    public MentorDto.ChatResponse chat(String email, MentorDto.ChatRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.userNotFound(email));

        ChatConversation conversation;

        if (request.getConversationId() != null) {
            conversation = conversationRepository.findByIdAndUserId(request.getConversationId(), user.getId())
                    .orElseThrow(() -> AppException.conversationNotFound(request.getConversationId()));
        } else {
            // New conversation — auto-title from first message
            String title = request.getMessage().length() > 50
                    ? request.getMessage().substring(0, 50) + "..."
                    : request.getMessage();

            conversation = ChatConversation.builder()
                    .user(user)
                    .title(title)
                    .build();
            conversation = conversationRepository.save(conversation);
        }

        // Build history for context
        List<ChatMessage> history = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
        List<Map<String, String>> historyForAi = new ArrayList<>();
        for (ChatMessage msg : history) {
            historyForAi.add(Map.of(
                    "role", msg.getRole().name().toLowerCase(),
                    "content", msg.getContent()
            ));
        }

        // Build user context
        String userContext = "User: " + user.getName() + " | Goal: " + user.getGoal() +
                " | Level: " + user.getExperienceLevel();

        // Call AI
        String reply = aiService.chat(request.getMessage(), historyForAi, userContext);

        // Save user message
        ChatMessage userMsg = ChatMessage.builder()
                .conversation(conversation)
                .role(ChatMessage.Role.USER)
                .content(request.getMessage())
                .build();
        messageRepository.save(userMsg);

        // Save AI reply
        ChatMessage assistantMsg = ChatMessage.builder()
                .conversation(conversation)
                .role(ChatMessage.Role.ASSISTANT)
                .content(reply)
                .build();
        messageRepository.save(assistantMsg);

        // Update conversation
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        return MentorDto.ChatResponse.builder()
                .conversationId(conversation.getId())
                .conversationTitle(conversation.getTitle())
                .reply(reply)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public List<MentorDto.ConversationSummary> getConversations(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.userNotFound(email));

        return conversationRepository
                .findByUserIdAndActiveTrueOrderByLastMessageAtDesc(user.getId(), PageRequest.of(0, 50))
                .stream()
                .map(c -> MentorDto.ConversationSummary.builder()
                        .id(c.getId())
                        .title(c.getTitle())
                        .lastMessageAt(c.getLastMessageAt())
                        .messageCount(c.getMessages().size())
                        .build())
                .toList();
    }

    public MentorDto.ConversationDetail getConversation(String email, Long conversationId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.userNotFound(email));

        ChatConversation conversation = conversationRepository
                .findByIdAndUserId(conversationId, user.getId())
                .orElseThrow(() -> AppException.conversationNotFound(conversationId));

        List<MentorDto.MessageDto> messages = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(m -> MentorDto.MessageDto.builder()
                        .id(m.getId())
                        .role(m.getRole().name().toLowerCase())
                        .content(m.getContent())
                        .createdAt(m.getCreatedAt())
                        .build())
                .toList();

        return MentorDto.ConversationDetail.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .messages(messages)
                .createdAt(conversation.getCreatedAt())
                .build();
    }

    @Transactional
    public void deleteConversation(String email, Long conversationId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.userNotFound(email));

        ChatConversation conversation = conversationRepository
                .findByIdAndUserId(conversationId, user.getId())
                .orElseThrow(() -> AppException.conversationNotFound(conversationId));

        conversation.setActive(false);
        conversationRepository.save(conversation);
    }

    @Transactional
    public MentorDto.CodeReviewResponse reviewCode(String email, MentorDto.CodeReviewRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.userNotFound(email));

        Map<String, Object> reviewResult = aiService.reviewCode(
                request.getCode(), request.getLanguage(),
                request.getProblemContext() != null ? request.getProblemContext() : "General review"
        );

        CodeReview codeReview = CodeReview.builder()
                .user(user)
                .originalCode(request.getCode())
                .language(request.getLanguage())
                .problemContext(request.getProblemContext())
                .overallScore(toInt(reviewResult.get("overallScore")))
                .grade(toString(reviewResult.get("grade")))
                .reviewResultJson(toJson(reviewResult))
                .build();

        Map<String, Object> betterSolution = null;
        if (request.isRequestBetterSolution()) {
            betterSolution = aiService.generateBetterSolution(
                    request.getCode(), request.getLanguage(),
                    request.getProblemContext() != null ? request.getProblemContext() : "General"
            );
            codeReview.setBetterSolutionJson(toJson(betterSolution));
            codeReview.setHasBetterSolution(true);
        }

        codeReview = codeReviewRepository.save(codeReview);

        return MentorDto.CodeReviewResponse.builder()
                .id(codeReview.getId())
                .language(codeReview.getLanguage())
                .problemContext(codeReview.getProblemContext())
                .overallScore(codeReview.getOverallScore())
                .grade(codeReview.getGrade())
                .review(reviewResult)
                .betterSolution(betterSolution)
                .createdAt(codeReview.getCreatedAt())
                .build();
    }

    public List<MentorDto.CodeReviewSummary> getCodeReviewHistory(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.userNotFound(email));

        return codeReviewRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, 20))
                .stream()
                .map(r -> MentorDto.CodeReviewSummary.builder()
                        .id(r.getId())
                        .language(r.getLanguage())
                        .problemContext(r.getProblemContext())
                        .overallScore(r.getOverallScore())
                        .grade(r.getGrade())
                        .createdAt(r.getCreatedAt())
                        .build())
                .toList();
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (JsonProcessingException e) { return "{}"; }
    }

    private String toString(Object obj) { return obj != null ? obj.toString() : null; }

    private Integer toInt(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Integer i) return i;
        if (obj instanceof Number n) return n.intValue();
        try { return Integer.parseInt(obj.toString()); } catch (NumberFormatException e) { return null; }
    }
}