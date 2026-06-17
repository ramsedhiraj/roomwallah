package com.roomwallah.trust;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomwallah.identity.application.service.CurrentUserProvider;
import com.roomwallah.user.entity.User;
import com.roomwallah.trust.application.facade.TrustFacade;
import com.roomwallah.trust.domain.entity.*;
import com.roomwallah.trust.presentation.TrustController;
import com.roomwallah.trust.presentation.TrustAdminController;
import com.roomwallah.trust.presentation.dto.VerificationSubmissionRequest;
import com.roomwallah.trust.presentation.dto.RejectionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TrustControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private TrustFacade trustFacade;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private ObjectMapper objectMapper;
    private User mockUser;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();

        // Setup controller – TrustController only takes (TrustFacade, CurrentUserProvider)
        TrustController trustController = new TrustController(trustFacade, currentUserProvider);
        TrustAdminController trustAdminController = new TrustAdminController(trustFacade, currentUserProvider);

        mockMvc = MockMvcBuilders.standaloneSetup(trustController, trustAdminController).build();

        // Setup mock user
        mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setEmail("owner@roomwallah.com");
        when(currentUserProvider.getCurrentUser()).thenReturn(mockUser);
    }

    @Test
    public void testSubmitVerification_Success() throws Exception {
        VerificationSubmissionRequest request = new VerificationSubmissionRequest();
        request.setLevel(VerificationLevel.STANDARD);
        request.setProvider(VerificationProvider.JUMIO);
        request.setMediaIds(List.of(UUID.randomUUID()));

        OwnerVerification mockVerif = OwnerVerification.builder()
                .userId(mockUser.getId())
                .verificationStatus(VerificationStatus.PENDING)
                .verificationLevel(VerificationLevel.STANDARD)
                .submittedAt(Instant.now())
                .build();
        mockVerif.setId(UUID.randomUUID());

        when(trustFacade.submitVerification(eq(mockUser.getId()), eq(VerificationLevel.STANDARD),
                eq(VerificationProvider.JUMIO), anyList(), any())).thenReturn(mockVerif);

        // Controller returns ApiResponse.success(...) which maps to HTTP 200
        mockMvc.perform(post("/api/v1/trust/verification")
                        .header("Idempotency-Key", "key_111")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.verificationStatus").value("PENDING"));
    }

    @Test
    public void testGetVerificationStatus_Success() throws Exception {
        OwnerVerification mockVerif = OwnerVerification.builder()
                .userId(mockUser.getId())
                .verificationStatus(VerificationStatus.APPROVED)
                .verificationLevel(VerificationLevel.VIP)
                .submittedAt(Instant.now())
                .build();

        when(trustFacade.getOwnerVerification(mockUser.getId())).thenReturn(Optional.of(mockVerif));

        mockMvc.perform(get("/api/v1/trust/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.verificationStatus").value("APPROVED"));
    }

    @Test
    public void testGetTrustScore_Success() throws Exception {
        TrustScore mockScore = new TrustScore();
        mockScore.setCurrentScore(85);
        mockScore.setScoreVersion(1);
        mockScore.setRuleVersion("1.0.0");
        mockScore.setAlgorithmVersion("1.0.0");
        mockScore.setExplanationJson("{\"verified\":true}");

        when(trustFacade.getTrustScore(mockUser.getId())).thenReturn(Optional.of(mockScore));

        mockMvc.perform(get("/api/v1/trust/score"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.currentScore").value(85));
    }

    @Test
    public void testAdminApproveVerification_Success() throws Exception {
        UUID verificationId = UUID.randomUUID();
        OwnerVerification approvedVerif = OwnerVerification.builder()
                .userId(UUID.randomUUID())
                .verificationStatus(VerificationStatus.APPROVED)
                .verificationLevel(VerificationLevel.VIP)
                .submittedAt(Instant.now())
                .build();

        when(trustFacade.approveVerification(eq(verificationId), eq(mockUser.getId()))).thenReturn(approvedVerif);

        mockMvc.perform(post("/api/v1/admin/trust/{id}/approve", verificationId)
                        .header("Idempotency-Key", "admin_key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.verificationStatus").value("APPROVED"));
    }

    @Test
    public void testAdminRejectVerification_Success() throws Exception {
        UUID verificationId = UUID.randomUUID();
        RejectionRequest request = new RejectionRequest();
        request.setReason("Face likeness too low");

        OwnerVerification rejectedVerif = OwnerVerification.builder()
                .userId(UUID.randomUUID())
                .verificationStatus(VerificationStatus.REJECTED)
                .verificationLevel(VerificationLevel.BASIC)
                .submittedAt(Instant.now())
                .build();

        when(trustFacade.rejectVerification(eq(verificationId), eq(mockUser.getId()), eq("Face likeness too low")))
                .thenReturn(rejectedVerif);

        mockMvc.perform(post("/api/v1/admin/trust/{id}/reject", verificationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.verificationStatus").value("REJECTED"));
    }
}
