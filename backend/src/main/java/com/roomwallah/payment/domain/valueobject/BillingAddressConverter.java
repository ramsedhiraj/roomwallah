package com.roomwallah.payment.domain.valueobject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class BillingAddressConverter implements AttributeConverter<BillingAddress, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(BillingAddress attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting BillingAddress to JSON string", e);
        }
    }

    @Override
    public BillingAddress convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(dbData, BillingAddress.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting JSON string to BillingAddress", e);
        }
    }
}
