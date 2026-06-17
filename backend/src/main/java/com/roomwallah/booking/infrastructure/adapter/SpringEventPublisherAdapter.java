package com.roomwallah.booking.infrastructure.adapter;

import com.roomwallah.booking.domain.port.EventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component("bookingSpringEventPublisherAdapter")
@RequiredArgsConstructor
public class SpringEventPublisherAdapter implements EventPublisherPort {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(Object event) {
        log.debug("Booking Event Publisher Port - Publishing: {}", event.getClass().getSimpleName());
        applicationEventPublisher.publishEvent(event);
    }
}
