package com.roomwallah.chat.application.service;

import com.roomwallah.chat.domain.entity.Conversation;
import com.roomwallah.chat.domain.entity.Message;
import com.roomwallah.chat.domain.event.MessageCreatedEvent;
import com.roomwallah.chat.domain.repository.ConversationRepository;
import com.roomwallah.chat.domain.repository.MessageRepository;
import com.roomwallah.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Conversation getOrCreateConversation(UUID bookingId, UUID tenantId, UUID ownerId) {
        log.info("Get or create conversation for booking: {}, tenant: {}, owner: {}", bookingId, tenantId, ownerId);
        return conversationRepository.findByBookingId(bookingId)
                .orElseGet(() -> {
                    Conversation conversation = Conversation.builder()
                            .bookingId(bookingId)
                            .tenantId(tenantId)
                            .ownerId(ownerId)
                            .build();
                    return conversationRepository.save(conversation);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Conversation> getUserConversations(UUID userId) {
        log.debug("Fetching conversations for user: {}", userId);
        return conversationRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public Message sendMessage(UUID conversationId, UUID senderId, String content) {
        log.info("Sending message in conversation: {} from sender: {}", conversationId, senderId);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (!conversation.getTenantId().equals(senderId) && !conversation.getOwnerId().equals(senderId)) {
            throw new SecurityException("Unauthorized chat sender");
        }

        Message message = Message.builder()
                .conversationId(conversationId)
                .senderId(senderId)
                .content(content)
                .read(false)
                .build();
        Message savedMessage = messageRepository.save(message);

        // Update conversation timestamp for sorting
        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);

        // Determine recipient
        UUID recipientId = conversation.getTenantId().equals(senderId) ? conversation.getOwnerId() : conversation.getTenantId();

        // Publish event
        MessageCreatedEvent event = MessageCreatedEvent.builder()
                .messageId(savedMessage.getId())
                .conversationId(conversationId)
                .senderId(senderId)
                .recipientId(recipientId)
                .content(content)
                .createdAt(savedMessage.getCreatedAt() != null ? savedMessage.getCreatedAt() : Instant.now())
                .build();
        eventPublisher.publishEvent(event);

        return savedMessage;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getConversationMessages(UUID conversationId, UUID userId) {
        log.debug("Fetching messages for conversation: {} by user: {}", conversationId, userId);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (!conversation.getTenantId().equals(userId) && !conversation.getOwnerId().equals(userId)) {
            throw new SecurityException("Unauthorized chat participant");
        }

        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    @Override
    @Transactional
    public void markConversationAsRead(UUID conversationId, UUID userId) {
        log.info("Marking conversation: {} messages as read by user: {}", conversationId, userId);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (!conversation.getTenantId().equals(userId) && !conversation.getOwnerId().equals(userId)) {
            throw new SecurityException("Unauthorized chat participant");
        }

        List<Message> unreadMessages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .filter(m -> !m.getSenderId().equals(userId) && !m.isRead())
                .toList();

        for (Message message : unreadMessages) {
            message.setRead(true);
        }
        messageRepository.saveAll(unreadMessages);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID conversationId, UUID userId) {
        return messageRepository.countUnreadMessages(conversationId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Message getLatestMessage(UUID conversationId) {
        List<Message> messages = messageRepository.findLatestMessage(conversationId);
        return messages.isEmpty() ? null : messages.get(0);
    }
}
