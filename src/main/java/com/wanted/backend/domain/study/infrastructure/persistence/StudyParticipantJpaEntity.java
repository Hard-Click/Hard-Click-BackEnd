package com.wanted.backend.domain.study.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(name = "study_participant")
@Getter
public class StudyParticipantJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "study_participant_id")
    private Long id;

    @Column(name = "study_id", nullable = false)
    private Long studyId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    protected StudyParticipantJpaEntity() {}

    public StudyParticipantJpaEntity(Long studyId, Long memberId, LocalDateTime joinedAt) {
        this.studyId = studyId;
        this.memberId = memberId;
        this.joinedAt = joinedAt;
    }
}
