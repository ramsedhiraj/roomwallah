package com.roomwallah;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomwallah.assistant.ChatController;
import com.roomwallah.assistant.ChatSession;
import com.roomwallah.assistant.ConversationalAssistantService;
import com.roomwallah.common.ai.feedback.AiBenchmarkService;
import com.roomwallah.common.ai.feedback.AiFeedback;
import com.roomwallah.common.ai.feedback.AiFeedbackRepository;
import com.roomwallah.common.ai.registry.PromptRegistry;
import com.roomwallah.common.observability.AiObservabilityService;
import com.roomwallah.common.observability.TenantAiQuota;
import com.roomwallah.common.observability.TenantAiQuotaRepository;
import com.roomwallah.identity.application.service.CurrentUserProvider;
import com.roomwallah.user.entity.User;
import com.roomwallah.user.entity.UserRole;
import com.roomwallah.user.entity.AccountStatus;
import com.roomwallah.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AiFeaturesExtensionTest {

    @Autowired
    private PromptRegistry promptRegistry;

    @Autowired
    private AiFeedbackRepository feedbackRepository;

    @Autowired
    private TenantAiQuotaRepository quotaRepository;

    @Autowired
    private AiObservabilityService observabilityService;

    @Autowired
    private AiBenchmarkService benchmarkService;

    @Autowired
    private ConversationalAssistantService assistantService;

    @Autowired
    private ChatController chatController;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    private User testUser;

    @BeforeEach
    public void setup() {
        testUser = new User();
        testUser.setEmail("test-" + UUID.randomUUID() + "@roomwallah.com");
        testUser.setFullName("Test User");
        testUser.setPasswordHash("hashed_password");
        testUser.setPhone("+919999999999");
        testUser.setRole(UserRole.TENANT);
        testUser.setStatus(AccountStatus.ACTIVE);
        testUser.setCreatedAt(Instant.now());
        testUser.setUpdatedAt(Instant.now());
        testUser = userRepository.saveAndFlush(testUser);

        when(currentUserProvider.getCurrentUser()).thenReturn(testUser);
    }

    @Test
    public void testPromptRegistryRollbackAndVersioning() {
        String initialVersion = promptRegistry.getActiveVersion("intent_parsing");
        assertThat(initialVersion).isEqualTo("v1");

        promptRegistry.rollbackOrSetVersion("intent_parsing", "v2");
        assertThat(promptRegistry.getActiveVersion("intent_parsing")).isEqualTo("v2");

        promptRegistry.rollbackOrSetVersion("intent_parsing", "v1");
        assertThat(promptRegistry.getActiveVersion("intent_parsing")).isEqualTo("v1");

        assertThrows(IllegalArgumentException.class, () -> {
            promptRegistry.rollbackOrSetVersion("intent_parsing", "v999");
        });
    }

    @Test
    public void testPerTenantBudgetQuotaEnforcement() {
        String testTenant = "test-tenant-" + UUID.randomUUID().toString();
        
        TenantAiQuota quota = TenantAiQuota.builder()
                .tenantId(testTenant)
                .monthlyLimitUsd(new BigDecimal("5.0000"))
                .currentSpendUsd(new BigDecimal("4.9990"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .build();
        quotaRepository.saveAndFlush(quota);

        observabilityService.verifyTenantQuota(testTenant, 0.0005);

        assertThrows(IllegalStateException.class, () -> {
            observabilityService.verifyTenantQuota(testTenant, 0.002);
        });
    }

    @Test
    public void testAiFeedbackPersistenceAndCollection() {
        String targetId = UUID.randomUUID().toString();
        AiFeedback feedback = AiFeedback.builder()
                .targetType("CHAT_MESSAGE")
                .targetId(targetId)
                .userId(testUser.getId())
                .isPositive(false)
                .issueReport("The answer did not contain correct rents for Noida Sector 62.")
                .createdAt(Instant.now())
                .version(0L)
                .build();

        AiFeedback saved = feedbackRepository.save(feedback);
        assertThat(saved.getId()).isNotNull();

        List<AiFeedback> queriedList = feedbackRepository.findByTargetId(targetId);
        assertThat(queriedList).hasSize(1);
        assertThat(queriedList.get(0).isPositive()).isFalse();
        assertThat(queriedList.get(0).getIssueReport()).contains("Noida Sector 62");
    }

    @Test
    public void testAiRegressionBenchmarks() {
        Map<String, Object> benchmarkResults = benchmarkService.runEvaluations();
        assertThat(benchmarkResults).containsKey("accuracy");
        assertThat(benchmarkResults).containsKey("averageLatencyMs");
        assertThat(benchmarkResults).containsKey("details");
        
        List<?> details = (List<?>) benchmarkResults.get("details");
        assertThat(details).isNotEmpty();
    }

    @Test
    public void testChatStreamingResponse() throws Exception {
        ChatSession session = assistantService.createSession(testUser.getId(), "Streaming Session");
        
        SseEmitter emitter = chatController.streamResponse(session.getId(), "Show rooms in Mumbai");
        assertThat(emitter).isNotNull();
        assertThat(emitter.getTimeout()).isEqualTo(60000L);
    }
}
