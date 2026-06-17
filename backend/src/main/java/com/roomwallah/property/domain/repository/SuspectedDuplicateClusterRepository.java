package com.roomwallah.property.domain.repository;

import com.roomwallah.property.domain.entity.SuspectedDuplicateCluster;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SuspectedDuplicateClusterRepository extends JpaRepository<SuspectedDuplicateCluster, String> {
    List<SuspectedDuplicateCluster> findByStatus(String status);
}
