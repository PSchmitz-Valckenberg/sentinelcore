package com.sentinelcore.repository;

import com.sentinelcore.domain.entity.EvaluationRun;
import com.sentinelcore.domain.enums.RunStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluationRunRepository extends JpaRepository<EvaluationRun, String> {
    List<EvaluationRun> findByStatus(RunStatus status);
}
