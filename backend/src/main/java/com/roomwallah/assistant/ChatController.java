package com.roomwallah.assistant;

import com.roomwallah.common.dto.ApiResponse;
import com.roomwallah.identity.application.service.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
@Tag(name = "AI Conversational Assistant", description = "AI Property Assistant and conversation threads")
public class ChatController {

    private final ConversationalAssistantService assistantService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/sessions")
    @Operation(summary = "Create a new conversation session thread")
    public ApiResponse<ChatSession> createSession(@RequestBody(required = false) Map<String, String> body) {
        UUID userId = currentUserProvider.getCurrentUser().getId();
        String title = body != null ? body.get("title") : null;
        ChatSession session = assistantService.createSession(userId, title);
        return ApiResponse.success(session, "Conversation session created successfully");
    }

    @GetMapping("/sessions")
    @Operation(summary = "Get all session threads for the authenticated user")
    public ApiResponse<List<ChatSession>> getUserSessions() {
        UUID userId = currentUserProvider.getCurrentUser().getId();
        List<ChatSession> sessions = assistantService.getUserSessions(userId);
        return ApiResponse.success(sessions, "User chat sessions retrieved successfully");
    }

    @PostMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "Send a message to the AI assistant in a session thread")
    public ApiResponse<Map<String, Object>> sendMessage(
            @PathVariable UUID sessionId,
            @RequestBody Map<String, String> body
    ) {
        UUID userId = currentUserProvider.getCurrentUser().getId();
        String content = body.get("content");
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
        String aiResponseJson = assistantService.sendMessage(sessionId, content, userId);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = new com.fasterxml.jackson.databind.ObjectMapper().readValue(aiResponseJson, Map.class);
            return ApiResponse.success(map, "AI response generated successfully");
        } catch (Exception e) {
            return ApiResponse.success(Map.of("response", aiResponseJson), "AI response generated successfully");
        }
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "Delete a specific conversation session thread")
    public ApiResponse<Map<String, Object>> deleteSession(@PathVariable UUID sessionId) {
        UUID userId = currentUserProvider.getCurrentUser().getId();
        assistantService.deleteSession(sessionId, userId);
        return ApiResponse.success(Map.of("deletedSessionId", sessionId), "Chat session deleted successfully");
    }

    @DeleteMapping("/conversations")
    @Operation(summary = "Delete all conversations and logs for the authenticated user")
    public ApiResponse<String> deleteAllConversations() {
        UUID userId = currentUserProvider.getCurrentUser().getId();
        assistantService.deleteUserConversations(userId);
        return ApiResponse.success("All conversation history deleted successfully");
    }

    @GetMapping("/export")
    @Operation(summary = "Export all user conversations in JSON format")
    public ApiResponse<List<Map<String, Object>>> exportConversations() {
        UUID userId = currentUserProvider.getCurrentUser().getId();
        List<Map<String, Object>> exported = assistantService.exportConversations(userId);
        return ApiResponse.success(exported, "Conversation history exported successfully");
    }

    @GetMapping(value = "/sessions/{sessionId}/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream AI assistant responses using Server-Sent Events (SSE)")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamResponse(
            @PathVariable UUID sessionId,
            @RequestParam String content
    ) {
        UUID userId = currentUserProvider.getCurrentUser().getId();
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(60000L);
        
        new Thread(() -> {
            try {
                String fullResponseJson = assistantService.sendMessage(sessionId, content, userId);
                String responseText;
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = new com.fasterxml.jackson.databind.ObjectMapper().readValue(fullResponseJson, Map.class);
                    responseText = (String) map.get("response");
                } catch (Exception e) {
                    responseText = fullResponseJson;
                }
                String[] words = responseText.split(" ");
                for (String word : words) {
                    emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                            .name("message")
                            .data(word + " "));
                    Thread.sleep(50); // Simulating streaming delay
                }
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                            .name("error")
                            .data("Error: " + e.getMessage()));
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        }).start();
        
        return emitter;
    }
}
