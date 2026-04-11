package com.sentinelcore.repository;

import com.sentinelcore.domain.entity.RagDocument;
import com.sentinelcore.domain.enums.DocumentTrustLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RagDocumentRepository extends JpaRepository<RagDocument, String> {
    List<RagDocument> findByTrustLevel(DocumentTrustLevel trustLevel);
}
