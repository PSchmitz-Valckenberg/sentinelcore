package com.sentinelcore.domain.entity;

import com.sentinelcore.domain.enums.AttackCategory;
import com.sentinelcore.domain.enums.CheckType;
import com.sentinelcore.domain.enums.EvaluationCaseType;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "evaluation_cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationCase {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "case_type", nullable = false)
    private EvaluationCaseType caseType;

    @Enumerated(EnumType.STRING)
    @Column(name = "attack_category")
    private AttackCategory attackCategory;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "user_input", nullable = false, columnDefinition = "TEXT")
    private String userInput;

    @Column(name = "expected_behavior", nullable = false, columnDefinition = "TEXT")
    private String expectedBehavior;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "evaluation_case_documents",
        joinColumns = @JoinColumn(name = "case_id"),
        inverseJoinColumns = @JoinColumn(name = "document_id")
    )
    @Builder.Default
    private Set<RagDocument> ragDocuments = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "evaluation_case_checks",
        joinColumns = @JoinColumn(name = "case_id")
    )
    @Column(name = "check_type")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<CheckType> relevantChecks = new HashSet<>();
}
