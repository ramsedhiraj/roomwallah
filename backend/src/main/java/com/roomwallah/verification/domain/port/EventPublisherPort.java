package com.roomwallah.verification.domain.port;

public interface EventPublisherPort {
    void publish(Object event);
}
