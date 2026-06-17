package com.roomwallah.trust.infrastructure.adapter;

import com.roomwallah.trust.domain.port.FaceMatchPort;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class StubFaceMatchAdapter implements FaceMatchPort {
    @Override
    public double matchFaces(UUID selfMediaId, UUID docPhotoMediaId) {
        return 0.96;
    }
}
