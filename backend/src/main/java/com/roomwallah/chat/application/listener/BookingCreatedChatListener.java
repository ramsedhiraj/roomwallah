package com.roomwallah.chat.application.listener;

import com.roomwallah.booking.domain.event.BookingCreatedEvent;
import com.roomwallah.chat.application.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingCreatedChatListener {

    private final ChatService chatService;

    @EventListener
    public void onBookingCreated(BookingCreatedEvent event) {
        log.info("BookingCreatedEvent received. Setting up chat room for booking ID: {}", event.getBookingId());
        try {
            chatService.getOrCreateConversation(
                    event.getBookingId(),
                    event.getTenantId(),
                    event.getOwnerId()
            );
            log.info("Successfully established chat room for booking ID: {}", event.getBookingId());
        } catch (Exception e) {
            log.error("Failed to automatically open chat room for booking ID: {}", event.getBookingId(), e);
        }
    }
}
