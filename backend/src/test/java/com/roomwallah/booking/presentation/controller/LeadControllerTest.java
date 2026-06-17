package com.roomwallah.booking.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomwallah.booking.application.facade.BookingFacade;
import com.roomwallah.booking.presentation.dto.LeadRequestDto;
import com.roomwallah.booking.presentation.dto.LeadResponseDto;
import com.roomwallah.identity.application.service.CurrentUserProvider;
import com.roomwallah.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class LeadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingFacade bookingFacade;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    private User mockUser;
    private UUID propertyId;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setEmail("tenant@example.com");
        mockUser.setPhone("+919999999999");
        
        propertyId = UUID.randomUUID();
        ownerId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUser()).thenReturn(mockUser);
    }

    @Test
    void createLead_withValidBody_returnsSuccess() throws Exception {
        LeadRequestDto request = LeadRequestDto.builder()
                .propertyId(propertyId)
                .ownerId(ownerId)
                .inquiryText("I am interested")
                .build();

        LeadResponseDto mockResponse = LeadResponseDto.builder()
                .id(UUID.randomUUID())
                .propertyId(propertyId)
                .tenantId(mockUser.getId())
                .ownerId(ownerId)
                .inquiryText("I am interested")
                .build();

        when(bookingFacade.getOrCreateLead(
                eq(propertyId),
                eq(mockUser.getId()),
                eq(ownerId),
                eq("I am interested"),
                eq(mockUser.getPhone()),
                eq(mockUser.getEmail())
        )).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.propertyId").value(propertyId.toString()));
    }

    @Test
    void getOwnerLeads_returnsList() throws Exception {
        LeadResponseDto mockLead = LeadResponseDto.builder()
                .id(UUID.randomUUID())
                .propertyId(propertyId)
                .ownerId(mockUser.getId())
                .build();

        when(bookingFacade.getOwnerLeads(mockUser.getId())).thenReturn(Collections.singletonList(mockLead));

        mockMvc.perform(get("/api/v1/leads/owner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].ownerId").value(mockUser.getId().toString()));
    }
}
