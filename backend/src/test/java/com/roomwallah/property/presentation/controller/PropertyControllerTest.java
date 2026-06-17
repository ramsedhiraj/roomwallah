package com.roomwallah.property.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomwallah.identity.application.service.CurrentUserProvider;
import com.roomwallah.property.application.facade.PropertyFacade;
import com.roomwallah.property.domain.entity.PropertyStatus;
import com.roomwallah.property.presentation.dto.PropertyResponse;
import com.roomwallah.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class PropertyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PropertyFacade propertyFacade;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    private User mockUser;
    private UUID propertyId;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setEmail("owner@example.com");
        
        propertyId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUser()).thenReturn(mockUser);
    }

    @Test
    void submitForVerification_success() throws Exception {
        PropertyResponse mockResponse = PropertyResponse.builder()
                .id(propertyId)
                .status(PropertyStatus.PENDING_VERIFICATION)
                .title("Beautiful Apartment")
                .build();

        when(propertyFacade.submitForVerification(propertyId)).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/properties/" + propertyId + "/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Property submitted for verification successfully"))
                .andExpect(jsonPath("$.data.status").value("PENDING_VERIFICATION"));
    }
}
