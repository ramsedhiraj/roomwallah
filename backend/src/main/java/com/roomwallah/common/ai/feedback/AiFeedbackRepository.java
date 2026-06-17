package com.roomwallah.common.ai.feedback;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AiFeedbackRepository extends JpaRepository<AiFeedback, UUID> {
    List<AiFeedback> findByTargetId(String targetId);
}
