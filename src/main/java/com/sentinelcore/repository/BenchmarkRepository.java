package com.sentinelcore.repository;

import com.sentinelcore.domain.entity.Benchmark;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BenchmarkRepository extends JpaRepository<Benchmark, String> {}