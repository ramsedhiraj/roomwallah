package com.roomwallah.booking.domain.port;

public interface EventPublisherPort {
    void publish(Object event);
}
