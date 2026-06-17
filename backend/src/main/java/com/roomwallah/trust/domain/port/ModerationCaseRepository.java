package com.roomwallah.trust.domain.port;

import com.roomwallah.trust.domain.entity.ModerationCase;
import com.roomwallah.trust.domain.entity.ModerationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ModerationCaseRepository extends JpaRepository<ModerationCase, UUID> {
    List<ModerationCase> findByStatus(ModerationStatus status);
}
