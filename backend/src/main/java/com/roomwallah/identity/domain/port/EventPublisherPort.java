package com.roomwallah.identity.domain.port;

public interface EventPublisherPort {
    void publish(Object event);
}
