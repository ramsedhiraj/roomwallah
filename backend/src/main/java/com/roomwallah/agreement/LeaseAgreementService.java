package com.roomwallah.agreement;

import com.roomwallah.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaseAgreementService {

    private final LeaseAgreementRepository agreementRepository;

    @Transactional
    public LeaseAgreement createAgreement(
            UUID propertyId,
            UUID tenantId,
            UUID ownerId,
            BigDecimal rentAmount,
            LocalDate startDate,
            LocalDate endDate,
            String content
    ) {
        log.info("Drafting new lease agreement for property: {}, tenant: {}", propertyId, tenantId);
        LeaseAgreement agreement = LeaseAgreement.builder()
                .propertyId(propertyId)
                .tenantId(tenantId)
                .ownerId(ownerId)
                .rentAmount(rentAmount)
                .startDate(startDate)
                .endDate(endDate)
                .agreementContent(content)
                .status("PENDING_SIGNATURE")
                .termsVersion(1)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return agreementRepository.save(agreement);
    }

    @Transactional
    public LeaseAgreement signAgreement(
            UUID agreementId,
            UUID userId,
            String signatureHash,
            String ip,
            String fingerprint
    ) {
        log.info("Recording signature for agreement: {}, user: {}", agreementId, userId);
        LeaseAgreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("Agreement not found: " + agreementId));

        if (!agreement.getTenantId().equals(userId) && !agreement.getOwnerId().equals(userId)) {
            throw new SecurityException("User is not a party to this agreement");
        }

        boolean alreadySigned = agreement.getSignatures().stream()
                .anyMatch(s -> s.getUserId().equals(userId));
        if (alreadySigned) {
            throw new IllegalStateException("User has already signed this agreement");
        }

        Signature signature = Signature.builder()
                .agreement(agreement)
                .userId(userId)
                .signatureHash(signatureHash)
                .signedAt(Instant.now())
                .ipAddress(ip)
                .deviceFingerprint(fingerprint)
                .build();

        agreement.getSignatures().add(signature);
        agreement.setUpdatedAt(Instant.now());

        boolean tenantSigned = agreement.getSignatures().stream().anyMatch(s -> s.getUserId().equals(agreement.getTenantId()));
        boolean ownerSigned = agreement.getSignatures().stream().anyMatch(s -> s.getUserId().equals(agreement.getOwnerId()));

        if (tenantSigned && ownerSigned) {
            agreement.setStatus("SIGNED");
            log.info("Agreement {} is fully signed!", agreementId);
        }

        return agreementRepository.save(agreement);
    }

    @Transactional
    public LeaseAgreement renewAgreement(UUID agreementId, LocalDate newEndDate) {
        log.info("Renewing lease agreement: {}, new end date: {}", agreementId, newEndDate);
        LeaseAgreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("Agreement not found"));

        if (!"SIGNED".equals(agreement.getStatus())) {
            throw new IllegalStateException("Can only renew fully signed agreements");
        }

        agreement.setEndDate(newEndDate);
        agreement.setTermsVersion(agreement.getTermsVersion() + 1);
        agreement.setStatus("PENDING_SIGNATURE");
        agreement.getSignatures().clear();
        agreement.setUpdatedAt(Instant.now());

        return agreementRepository.save(agreement);
    }

    @Transactional
    public LeaseAgreement amendAgreement(UUID agreementId, String newContent) {
        log.info("Amending lease agreement: {}", agreementId);
        LeaseAgreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("Agreement not found"));

        agreement.setAgreementContent(newContent);
        agreement.setTermsVersion(agreement.getTermsVersion() + 1);
        agreement.setStatus("PENDING_SIGNATURE");
        agreement.getSignatures().clear();
        agreement.setUpdatedAt(Instant.now());

        return agreementRepository.save(agreement);
    }

    public List<LeaseAgreement> getTenantAgreements(UUID tenantId) {
        return agreementRepository.findByTenantId(tenantId);
    }

    public List<LeaseAgreement> getOwnerAgreements(UUID ownerId) {
        return agreementRepository.findByOwnerId(ownerId);
    }

    public LeaseAgreement getAgreement(UUID id) {
        return agreementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agreement not found: " + id));
    }
}
