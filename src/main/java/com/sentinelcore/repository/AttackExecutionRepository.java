package com.sentinelcore.repository;

import com.sentinelcore.domain.entity.AttackExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttackExecutionRepository extends JpaRepository<AttackExecution, String> {
    List<AttackExecution> findByRunId(String runId);
    long countByRunId(String runId);
}
