package com.roomwallah.experiment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExperimentService {

    private final ExperimentCohortRepository cohortRepository;
    private final Map<String, Integer> activeExperiments = new ConcurrentHashMap<>();

    @Transactional
    public String getOrAssignCohort(String experimentName, UUID userId, int treatmentTrafficPercent) {
        if (!activeExperiments.containsKey(experimentName)) {
            return "CONTROL";
        }

        Optional<ExperimentCohort> existing = cohortRepository.findByExperimentNameAndUserId(experimentName, userId);
        if (existing.isPresent()) {
            return existing.get().getCohort();
        }

        String hashInput = userId.toString() + ":" + experimentName;
        int bucket = (hashInput.hashCode() & Integer.MAX_VALUE) % 100;
        String cohort = bucket < treatmentTrafficPercent ? "TREATMENT" : "CONTROL";

        ExperimentCohort newCohort = ExperimentCohort.builder()
                .experimentName(experimentName)
                .userId(userId)
                .cohort(cohort)
                .assignedAt(Instant.now())
                .build();
        cohortRepository.save(newCohort);

        log.info("Assigned user {} to cohort {} for experiment {}", userId, cohort, experimentName);
        return cohort;
    }

    public void startExperiment(String name, int treatmentPercent) {
        log.info("Starting experiment: {} with {}% treatment traffic", name, treatmentPercent);
        activeExperiments.put(name, treatmentPercent);
    }

    public void stopExperiment(String name) {
        log.info("Stopping experiment: {}", name);
        activeExperiments.remove(name);
    }

    @Transactional
    public void rollbackExperiment(String name) {
        log.info("Rolling back experiment: {} (deleting cohorts & stopping)", name);
        activeExperiments.remove(name);
        List<ExperimentCohort> cohorts = cohortRepository.findAll();
        cohorts.stream()
                .filter(c -> c.getExperimentName().equals(name))
                .forEach(cohortRepository::delete);
    }

    public Map<String, Object> getExperimentStats(String name) {
        List<ExperimentCohort> cohorts = cohortRepository.findAll();
        long treatmentCount = cohorts.stream()
                .filter(c -> c.getExperimentName().equals(name) && "TREATMENT".equals(c.getCohort()))
                .count();
        long controlCount = cohorts.stream()
                .filter(c -> c.getExperimentName().equals(name) && "CONTROL".equals(c.getCohort()))
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("experimentName", name);
        stats.put("isActive", activeExperiments.containsKey(name));
        stats.put("treatmentCount", treatmentCount);
        stats.put("controlCount", controlCount);
        stats.put("totalAssigned", treatmentCount + controlCount);
        return stats;
    }

    public List<Map<String, Object>> listAllExperiments() {
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> allNames = new HashSet<>(activeExperiments.keySet());
        
        cohortRepository.findAll().forEach(c -> allNames.add(c.getExperimentName()));

        for (String name : allNames) {
            result.add(getExperimentStats(name));
        }
        return result;
    }
}
