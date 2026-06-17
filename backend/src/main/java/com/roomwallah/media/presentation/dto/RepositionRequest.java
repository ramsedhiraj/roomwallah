package com.roomwallah.media.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepositionRequest {
    private UUID prevMediaId;
    private UUID nextMediaId;
}
