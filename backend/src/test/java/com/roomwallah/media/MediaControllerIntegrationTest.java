package com.roomwallah.media;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomwallah.identity.application.service.CurrentUserProvider;
import com.roomwallah.media.application.facade.MediaFacade;

import com.roomwallah.media.domain.entity.PropertyMedia;
import com.roomwallah.media.domain.port.MediaStoragePort;
import com.roomwallah.media.domain.repository.PropertyMediaRepository;
import com.roomwallah.media.presentation.dto.ReorderRequest;
import com.roomwallah.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class MediaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MediaFacade mediaFacade;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    @MockBean
    private MediaStoragePort mediaStoragePort;

    @MockBean
    private PropertyMediaRepository propertyMediaRepository;

    private UUID userId;
    private User mockUser;

    @BeforeEach
    public void setUp() {
        userId = UUID.randomUUID();
        mockUser = new User();
        mockUser.setId(userId);
        mockUser.setEmail("owner@roomwallah.com");
        
        when(currentUserProvider.getCurrentUser()).thenReturn(mockUser);
    }

    @Test
    public void getPropertyMedia_returnsList() throws Exception {
        UUID propertyId = UUID.randomUUID();
        
        when(mediaFacade.getPropertyMedia(propertyId)).thenReturn(new ArrayList<>());
        when(mediaStoragePort.generateUrl(anyString())).thenReturn("/relative/url");

        mockMvc.perform(get("/api/v1/media/properties/" + propertyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Media retrieved successfully"));

        verify(mediaFacade).getPropertyMedia(propertyId);
    }

    @Test
    @WithMockUser
    public void deleteMedia_returnsSuccess() throws Exception {
        UUID mediaId = UUID.randomUUID();

        doNothing().when(mediaFacade).deleteMedia(mediaId, userId);

        mockMvc.perform(delete("/api/v1/media/" + mediaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Media deleted successfully"));

        verify(mediaFacade).deleteMedia(mediaId, userId);
    }

    @Test
    @WithMockUser
    public void setCoverImage_returnsSuccess() throws Exception {
        UUID mediaId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();

        PropertyMedia mockMedia = new PropertyMedia();
        mockMedia.setId(mediaId);
        mockMedia.setPropertyId(propertyId);

        when(propertyMediaRepository.findByIdAndDeletedFalse(mediaId)).thenReturn(Optional.of(mockMedia));
        doNothing().when(mediaFacade).setCoverImage(propertyId, userId, mediaId);

        mockMvc.perform(patch("/api/v1/media/" + mediaId + "/cover"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cover image updated successfully"));

        verify(mediaFacade).setCoverImage(propertyId, userId, mediaId);
    }

    @Test
    @WithMockUser
    public void reorderMedia_returnsSuccess() throws Exception {
        UUID propertyId = UUID.randomUUID();
        List<UUID> mediaIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        ReorderRequest request = new ReorderRequest(propertyId, mediaIds);

        doNothing().when(mediaFacade).reorderMedia(propertyId, userId, mediaIds);

        mockMvc.perform(patch("/api/v1/media/reorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Media reordered successfully"));

        verify(mediaFacade).reorderMedia(propertyId, userId, mediaIds);
    }
}
