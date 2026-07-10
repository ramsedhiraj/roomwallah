package com.roomwallah.chat.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageCreatedEvent {
    private UUID messageId;
    private UUID conversationId;
    private UUID senderId;
    private UUID recipientId;
    private String content;
    private Instant createdAt;
}
