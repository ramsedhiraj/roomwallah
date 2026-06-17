package com.roomwallah.search.presentation.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class AutoCompleteResponseDto {
    List<String> suggestions;
}
