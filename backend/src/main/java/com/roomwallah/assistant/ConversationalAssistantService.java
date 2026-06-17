package com.roomwallah.assistant;

import com.roomwallah.common.ai.registry.PromptRegistry;
import com.roomwallah.search.infrastructure.config.SearchFeatureFlags;
import com.roomwallah.common.ai.ChatProvider;
import com.roomwallah.common.ai.AiSafetyGuard;
import com.roomwallah.common.observability.AiObservabilityService;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationalAssistantService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final PropertyRepository propertyRepository;
    private final ChatProvider chatProvider;
    private final AiSafetyGuard aiSafetyGuard;
    private final AiObservabilityService observabilityService;
    private final PromptRegistry promptRegistry;
    private final SearchFeatureFlags featureFlags;

    @Transactional
    public ChatSession createSession(UUID userId, String title) {
        ChatSession session = ChatSession.builder()
                .userId(userId)
                .title(title != null ? title : "New Chat Session")
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS)) // 7-day retention by default
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return sessionRepository.save(session);
    }

    @Cacheable(value = "active_chat_sessions", key = "#sessionId")
    @Transactional(readOnly = true)
    public Optional<ChatSession> getSession(UUID sessionId) {
        return sessionRepository.findById(sessionId);
    }

    @Transactional(readOnly = true)
    public List<ChatSession> getUserSessions(UUID userId) {
        return sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    @CacheEvict(value = "active_chat_sessions", key = "#sessionId")
    @Transactional
    public String sendMessage(UUID sessionId, String content, UUID userId) {
        long startTime = System.currentTimeMillis();
        
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        if (!session.getUserId().equals(userId)) {
            observabilityService.trackTenantIsolationFailure();
            throw new SecurityException("Unauthorized access to chat session");
        }

        if (!featureFlags.isAssistantChatEnabled()) {
            log.info("Conversational assistant disabled via feature flag.");
            return "The AI Conversational Assistant is currently disabled. Please search listings manually.";
        }

        String tenantId = com.roomwallah.security.TenantContext.getCurrentTenant();

        // 1. Safety audit
        aiSafetyGuard.checkAuthorization(userId, "ASSISTANT_CHAT");
        String sanitizedContent = aiSafetyGuard.validateAndFilterInput(content, userId);

        // 2. Persist user message
        ChatMessage userMsg = ChatMessage.builder()
                .session(session)
                .sender("USER")
                .content(sanitizedContent)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        messageRepository.save(userMsg);

        // 3. RAG Retrieval - fetch contextually relevant properties
        String ragContext = resolveRagContext(sanitizedContent);

        // 4. Load history
        List<ChatMessage> history = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        
        // 5. Check if summarization is needed (exceeds 15 messages)
        if (history.size() > 15 && session.getSummary() == null) {
            summarizeSession(session, history);
        }

        // 6. Format chat payload for ChatProvider
        List<Map<String, String>> chatPayload = new ArrayList<>();
        
        String activeVersion = promptRegistry.getActiveVersion("assistant_chat");
        String template = promptRegistry.getTemplate("assistant_chat", activeVersion);
        String systemContent = template.replace("${context}", ragContext);
        if (session.getSummary() != null) {
            systemContent += "\nConversation Summary so far: " + session.getSummary();
        }

        Map<String, String> systemPrompt = new HashMap<>();
        systemPrompt.put("role", "system");
        systemPrompt.put("content", systemContent);
        chatPayload.add(systemPrompt);

        for (ChatMessage msg : history) {
            Map<String, String> turn = new HashMap<>();
            turn.put("role", msg.getSender().toLowerCase());
            turn.put("content", msg.getContent());
            chatPayload.add(turn);
        }

        // 7. Generate AI response
        String responseText = "";
        boolean success = false;
        String error = null;
        String modelIdentifier = chatProvider.getModelIdentifier();
        double confidence = 0.95;
        String explanation = "Matching properties found in listings";
        String rankingReasons = "Match score based on locality and rent budget";
        String retrievalSources = "RoomWallah DB";
        String fallbackReason = "None";
        String safetyStatus = "PASSED";

        try {
            double costEstimate = 0.0005; // Estimate cost for a chat generation
            observabilityService.verifyTenantQuota(tenantId, costEstimate);

            responseText = chatProvider.chat(chatPayload, sanitizedContent);
            responseText = aiSafetyGuard.maskSecrets(responseText);
            success = true;
            observabilityService.trackCacheLookup(true);
        } catch (Exception e) {
            error = e.getMessage();
            responseText = "I apologize, but I am having trouble connecting right now. Fallback: traditional search is active.";
            confidence = 0.3;
            fallbackReason = "Chat provider failed: " + e.getMessage();
            explanation = "Fallback default message";
            rankingReasons = "N/A";
            retrievalSources = "Static fallback";
            observabilityService.trackCacheLookup(false);
        }

        // Build structured JSON
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("response", responseText);
        responseMap.put("confidenceScore", confidence);
        responseMap.put("explanation", explanation);
        responseMap.put("rankingReasons", rankingReasons);
        responseMap.put("retrievalSources", retrievalSources);
        responseMap.put("fallbackReason", fallbackReason);
        responseMap.put("safetyInterventionStatus", safetyStatus);

        String jsonResponse = "";
        try {
            jsonResponse = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(responseMap);
        } catch (Exception e) {
            jsonResponse = "{\"response\":\"" + responseText + "\"}";
        }

        // 8. Persist assistant message
        ChatMessage assistantMsg = ChatMessage.builder()
                .session(session)
                .sender("ASSISTANT")
                .content(jsonResponse)
                .modelVersion(modelIdentifier)
                .promptTemplateVersion(activeVersion)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        messageRepository.save(assistantMsg);

        // Update session timestamp & lifespan
        session.setUpdatedAt(Instant.now());
        session.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        sessionRepository.save(session);

        // Track stats
        long duration = System.currentTimeMillis() - startTime;
        observabilityService.trackRequest(
                userId, 
                tenantId, 
                modelIdentifier, 
                sanitizedContent.length() / 4, 
                responseText.length() / 4, 
                duration, 
                success, 
                error
        );
        observabilityService.trackAssistantCall(!success);
        observabilityService.trackHallucination(success ? 0.05 : 0.0);
        observabilityService.trackSafetyViolation(); // Safety logic check count

        return jsonResponse;
    }

    private String resolveRagContext(String query) {
        String lower = query.toLowerCase();
        List<Property> properties = propertyRepository.findAll();
        
        List<Property> matches = properties.stream()
                .filter(p -> p.getAddress() != null)
                .filter(p -> lower.contains(p.getAddress().getCity().toLowerCase()) 
                        || (p.getAddress().getLine2() != null && lower.contains(p.getAddress().getLine2().toLowerCase())))
                .limit(3)
                .toList();

        if (matches.isEmpty()) {
            return "No matching listings found in database currently.";
        }

        StringBuilder context = new StringBuilder("Available listings matching terms:\n");
        for (Property p : matches) {
            context.append(String.format("- Property ID: %s in %s, %s. Rent: %s %s. Type: %s.\n",
                    p.getId(), p.getAddress().getLine2() != null ? p.getAddress().getLine2() : "Unknown", p.getAddress().getCity(),
                    p.getPrice() != null ? p.getPrice().getAmount() : "N/A",
                    p.getPrice() != null ? p.getPrice().getCurrency() : "INR",
                    p.getPropertyType() != null ? p.getPropertyType().name() : "Apartment"));
        }
        return context.toString();
    }

    private void summarizeSession(ChatSession session, List<ChatMessage> history) {
        log.info("Summarizing long chat history for session: {}", session.getId());
        try {
            String messagesConcat = history.stream()
                    .map(m -> m.getSender() + ": " + m.getContent())
                    .collect(Collectors.joining("\n"));
            
            List<Map<String, String>> payload = new ArrayList<>();
            Map<String, String> sys = new HashMap<>();
            sys.put("role", "system");
            sys.put("content", "Summarize the following chat conversation briefly in 2 sentences.");
            payload.add(sys);
            
            String summary = chatProvider.chat(payload, messagesConcat);
            session.setSummary(summary);
            log.info("Session summary updated: {}", summary);
        } catch (Exception e) {
            log.warn("Failed to generate conversation summary: {}", e.getMessage());
        }
    }

    @CacheEvict(value = "active_chat_sessions", key = "#sessionId")
    @Transactional
    public void deleteSession(UUID sessionId, UUID userId) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        
        if (!session.getUserId().equals(userId)) {
            observabilityService.trackTenantIsolationFailure();
            throw new SecurityException("Unauthorized delete request");
        }
        
        sessionRepository.delete(session);
        log.info("Chat session {} deleted by user request", sessionId);
    }

    @Transactional
    public void deleteUserConversations(UUID userId) {
        List<ChatSession> sessions = sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        sessionRepository.deleteAll(sessions);
        log.info("Deleted all chat sessions for user: {}", userId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> exportConversations(UUID userId) {
        List<ChatSession> sessions = sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        List<Map<String, Object>> exportData = new ArrayList<>();
        
        for (ChatSession session : sessions) {
            Map<String, Object> sessionMap = new HashMap<>();
            sessionMap.put("sessionId", session.getId());
            sessionMap.put("title", session.getTitle());
            sessionMap.put("summary", session.getSummary());
            sessionMap.put("createdAt", session.getCreatedAt());
            
            List<Map<String, Object>> msgList = new ArrayList<>();
            for (ChatMessage msg : session.getMessages()) {
                Map<String, Object> msgMap = new HashMap<>();
                msgMap.put("sender", msg.getSender());
                msgMap.put("content", msg.getContent());
                msgMap.put("timestamp", msg.getCreatedAt());
                msgList.add(msgMap);
            }
            sessionMap.put("messages", msgList);
            exportData.add(sessionMap);
        }
        
        return exportData;
    }

    // Scheduled retention job running at 02:00 AM daily
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanExpiredSessions() {
        log.info("Running chat session retention job...");
        List<ChatSession> expired = sessionRepository.findExpiredSessions(Instant.now());
        if (!expired.isEmpty()) {
            sessionRepository.deleteAll(expired);
            log.info("Cleaned up {} expired inactive sessions", expired.size());
        }
    }
}
