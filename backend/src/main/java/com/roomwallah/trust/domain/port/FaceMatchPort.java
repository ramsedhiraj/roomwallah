package com.roomwallah.trust.domain.port;

import java.util.UUID;

public interface FaceMatchPort {
    double matchFaces(UUID selfMediaId, UUID docPhotoMediaId);
}
