package com.roomwallah.chat.presentation.dto;

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
public class ConversationResponseDto {
    private UUID id;
    private UUID bookingId;
    private UUID tenantId;
    private UUID ownerId;
    private String tenantName;
    private String ownerName;
    private String latestMessage;
    private Instant latestMessageTime;
    private long unreadCount;
}
