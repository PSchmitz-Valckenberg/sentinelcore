package com.sentinelcore.repository;

import com.sentinelcore.domain.entity.EvaluationCase;
import com.sentinelcore.domain.enums.EvaluationCaseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluationCaseRepository extends JpaRepository<EvaluationCase, String> {
    List<EvaluationCase> findByCaseType(EvaluationCaseType caseType);
}
