package com.roomwallah.booking.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeadNoteRequestDto {
    @NotBlank(message = "Note content must not be blank")
    private String content;
}
