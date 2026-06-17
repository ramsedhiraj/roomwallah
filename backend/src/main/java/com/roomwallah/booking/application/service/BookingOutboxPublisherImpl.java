package com.roomwallah.booking.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomwallah.booking.domain.entity.BookingOutbox;
import com.roomwallah.booking.domain.repository.BookingOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingOutboxPublisherImpl implements BookingOutboxPublisher {

    private final BookingOutboxRepository bookingOutboxRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void publishEvents() {
        log.debug("Polling booking outbox events");
        List<BookingOutbox> pendingEvents = bookingOutboxRepository.findByStatus("PENDING");

        for (BookingOutbox outbox : pendingEvents) {
            try {
                publishEvent(outbox);
            } catch (Exception e) {
                log.error("Failed to publish outbox event ID: {}", outbox.getId(), e);
                outbox.setStatus("FAILED");
                bookingOutboxRepository.save(outbox);
            }
        }
    }

    private void publishEvent(BookingOutbox outbox) throws Exception {
        String className = "com.roomwallah.booking.domain.event." + outbox.getEventType();
        log.debug("Loading event class: {}", className);
        Class<?> eventClass = Class.forName(className);
        Object eventObj = objectMapper.readValue(outbox.getPayloadJson(), eventClass);

        applicationEventPublisher.publishEvent(eventObj);

        outbox.setStatus("PROCESSED");
        bookingOutboxRepository.save(outbox);
        log.info("Published outbox event ID: {} type: {}", outbox.getId(), outbox.getEventType());
    }
}
