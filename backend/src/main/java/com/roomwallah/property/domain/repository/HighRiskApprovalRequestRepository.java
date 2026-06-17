package com.roomwallah.property.domain.repository;

import com.roomwallah.property.domain.entity.HighRiskApprovalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface HighRiskApprovalRequestRepository extends JpaRepository<HighRiskApprovalRequest, UUID> {
    List<HighRiskApprovalRequest> findByStatus(String status);
}
