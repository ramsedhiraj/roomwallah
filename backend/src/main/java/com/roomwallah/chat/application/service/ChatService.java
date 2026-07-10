package com.roomwallah.chat.application.service;

import com.roomwallah.chat.domain.entity.Conversation;
import com.roomwallah.chat.domain.entity.Message;

import java.util.List;
import java.util.UUID;

public interface ChatService {
    Conversation getOrCreateConversation(UUID bookingId, UUID tenantId, UUID ownerId);
    List<Conversation> getUserConversations(UUID userId);
    Message sendMessage(UUID conversationId, UUID senderId, String content);
    List<Message> getConversationMessages(UUID conversationId, UUID userId);
    void markConversationAsRead(UUID conversationId, UUID userId);
    long getUnreadCount(UUID conversationId, UUID userId);
    Message getLatestMessage(UUID conversationId);
}
