package com.roomwallah.search.domain.event;

import lombok.Value;
import java.util.UUID;

@Value
public class PropertyReindexedEvent {
    UUID propertyId;
    long version;
}
