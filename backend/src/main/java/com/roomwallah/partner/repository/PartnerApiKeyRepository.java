package com.roomwallah.partner.repository;

import com.roomwallah.partner.domain.PartnerApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PartnerApiKeyRepository extends JpaRepository<PartnerApiKey, UUID> {
    Optional<PartnerApiKey> findByApiKeyHash(String hash);
}
