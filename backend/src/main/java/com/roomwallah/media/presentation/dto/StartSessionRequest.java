package com.roomwallah.media.presentation.dto;

import com.roomwallah.media.domain.entity.MediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartSessionRequest {
    @NotNull
    private UUID propertyId;
    
    @NotBlank
    private String filename;
    
    @NotNull
    private Long totalSize;
    
    @NotNull
    private MediaType mediaType;
}
