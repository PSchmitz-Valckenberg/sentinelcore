package com.sentinelcore.repository;

import com.sentinelcore.domain.entity.ScoreDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ScoreDetailRepository extends JpaRepository<ScoreDetail, String> {
    List<ScoreDetail> findByExecutionId(String executionId);
    List<ScoreDetail> findByExecutionIdIn(Collection<String> executionIds);
}