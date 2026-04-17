package com.sentinelcore.repository;

import com.sentinelcore.domain.entity.ScoreDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ScoreDetailRepository extends JpaRepository<ScoreDetail, String> {
    List<ScoreDetail> findByExecutionId(String executionId);
    List<ScoreDetail> findByExecutionIdIn(Collection<String> executionIds);
}