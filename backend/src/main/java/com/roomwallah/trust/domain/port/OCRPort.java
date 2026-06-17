package com.roomwallah.trust.domain.port;

import java.util.UUID;

public interface OCRPort {
    String extractText(UUID mediaId);
}
