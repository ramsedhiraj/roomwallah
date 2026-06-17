package com.roomwallah.search.domain.event;

import lombok.Value;
import java.util.UUID;

@Value
public class PropertyIndexedEvent {
    UUID propertyId;
    long version;
}
