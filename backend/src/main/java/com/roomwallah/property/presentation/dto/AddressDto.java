package com.roomwallah.property.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressDto {
    private String line1;
    private String line2;
    private String city;
    private String state;
    private String country;
    private String zipCode;
}
