package com.sentinelcore.domain.entity;

import com.sentinelcore.domain.enums.CheckType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "score_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoreDetail {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_id", nullable = false)
    private AttackExecution execution;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_type", nullable = false)
    private CheckType checkType;

    @Column(name = "result", nullable = false)
    private boolean result;

    @Column(name = "evidence", nullable = false, columnDefinition = "TEXT")
    private String evidence;
}
