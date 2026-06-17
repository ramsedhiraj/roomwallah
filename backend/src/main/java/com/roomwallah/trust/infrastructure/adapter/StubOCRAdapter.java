package com.roomwallah.trust.infrastructure.adapter;

import com.roomwallah.trust.domain.port.OCRPort;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class StubOCRAdapter implements OCRPort {
    @Override
    public String extractText(UUID mediaId) {
        return "DOCUMENT TYPE: PASSPORT; NAME: JOHN OWNER; NUMBER: A12345678; DOB: 1980-01-01; EXPIRY: 2030-01-01;";
    }
}
