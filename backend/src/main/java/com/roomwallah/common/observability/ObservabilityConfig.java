package com.roomwallah.common.observability;

import com.roomwallah.fraud.repository.FraudCaseRepository;
import com.roomwallah.partner.repository.PartnerApiKeyRepository;
import com.roomwallah.property.domain.repository.PropertyRepository;
import com.roomwallah.property.domain.entity.PropertyStatus;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {

    public ObservabilityConfig(
            MeterRegistry meterRegistry,
            PropertyRepository propertyRepository,
            FraudCaseRepository fraudCaseRepository,
            PartnerApiKeyRepository partnerApiKeyRepository
    ) {
        Gauge.builder("roomwallah.listings.active", propertyRepository, repo -> 
            repo.findAll().stream()
                .filter(p -> p.getStatus() == PropertyStatus.ACTIVE && !p.isDeleted())
                .count()
        )
        .description("Number of active property listings")
        .register(meterRegistry);

        Gauge.builder("roomwallah.fraud.cases.pending", fraudCaseRepository, repo -> 
            repo.findByStatus("PENDING_REVIEW").size()
        )
        .description("Number of pending fraud cases")
        .register(meterRegistry);

        Gauge.builder("roomwallah.partner.keys.active", partnerApiKeyRepository, repo -> 
            repo.findAll().stream().filter(k -> k.isEnabled() && !k.isRevoked()).count()
        )
        .description("Number of active partner API keys")
        .register(meterRegistry);
    }
}
