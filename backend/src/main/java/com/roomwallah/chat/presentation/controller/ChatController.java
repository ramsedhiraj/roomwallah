package com.roomwallah.chat.presentation.controller;

import com.roomwallah.chat.application.service.ChatService;
import com.roomwallah.chat.domain.entity.Conversation;
import com.roomwallah.chat.domain.entity.Message;
import com.roomwallah.chat.presentation.dto.ConversationResponseDto;
import com.roomwallah.chat.presentation.dto.MessageRequestDto;
import com.roomwallah.chat.presentation.dto.MessageResponseDto;
import com.roomwallah.common.dto.ApiResponse;
import com.roomwallah.identity.infrastructure.provider.UserPrincipal;
import com.roomwallah.user.entity.User;
import com.roomwallah.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final UserRepository userRepository;

    private UUID getLoggedInUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.user().getId();
        }
        throw new IllegalStateException("User not authenticated");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ConversationResponseDto>>> getConversations() {
        UUID userId = getLoggedInUserId();
        log.info("Fetching conversations list for user: {}", userId);
        List<Conversation> conversations = chatService.getUserConversations(userId);

        List<ConversationResponseDto> response = conversations.stream().map(c -> {
            User tenant = userRepository.findById(c.getTenantId()).orElse(null);
            User owner = userRepository.findById(c.getOwnerId()).orElse(null);
            String tenantName = tenant != null ? tenant.getFullName() : "Unknown Tenant";
            String ownerName = owner != null ? owner.getFullName() : "Unknown Owner";

            Message latest = chatService.getLatestMessage(c.getId());
            String latestText = latest != null ? latest.getContent() : null;
            java.time.Instant latestTime = latest != null ? latest.getCreatedAt() : c.getUpdatedAt();

            long unread = chatService.getUnreadCount(c.getId(), userId);

            return ConversationResponseDto.builder()
                    .id(c.getId())
                    .bookingId(c.getBookingId())
                    .tenantId(c.getTenantId())
                    .ownerId(c.getOwnerId())
                    .tenantName(tenantName)
                    .ownerName(ownerName)
                    .latestMessage(latestText)
                    .latestMessageTime(latestTime)
                    .unreadCount(unread)
                    .build();
        }).toList();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<List<MessageResponseDto>>> getMessages(@PathVariable("id") UUID conversationId) {
        UUID userId = getLoggedInUserId();
        log.info("Fetching messages for conversation: {} by user: {}", conversationId, userId);
        List<Message> messages = chatService.getConversationMessages(conversationId, userId);

        List<MessageResponseDto> response = messages.stream().map(m -> MessageResponseDto.builder()
                .id(m.getId())
                .senderId(m.getSenderId())
                .content(m.getContent())
                .read(m.isRead())
                .createdAt(m.getCreatedAt())
                .build()).toList();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<MessageResponseDto>> sendMessage(
            @PathVariable("id") UUID conversationId,
            @Valid @RequestBody MessageRequestDto request) {
        UUID userId = getLoggedInUserId();
        log.info("Sending message in conversation: {} by sender: {}", conversationId, userId);
        Message message = chatService.sendMessage(conversationId, userId, request.getContent());

        MessageResponseDto response = MessageResponseDto.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .read(message.isRead())
                .createdAt(message.getCreatedAt())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Message sent successfully"));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable("id") UUID conversationId) {
        UUID userId = getLoggedInUserId();
        log.info("Marking messages read for conversation: {} by user: {}", conversationId, userId);
        chatService.markConversationAsRead(conversationId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Conversation marked as read"));
    }
}
