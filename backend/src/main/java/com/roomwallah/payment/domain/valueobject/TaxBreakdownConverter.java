package com.roomwallah.payment.domain.valueobject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TaxBreakdownConverter implements AttributeConverter<TaxBreakdown, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(TaxBreakdown attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting TaxBreakdown to JSON string", e);
        }
    }

    @Override
    public TaxBreakdown convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(dbData, TaxBreakdown.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting JSON string to TaxBreakdown", e);
        }
    }
}
