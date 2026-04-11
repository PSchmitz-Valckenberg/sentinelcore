package com.sentinelcore.domain.entity;

import com.sentinelcore.domain.enums.DocumentTrustLevel;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rag_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RagDocument {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "trust_level", nullable = false)
    private DocumentTrustLevel trustLevel;
}
