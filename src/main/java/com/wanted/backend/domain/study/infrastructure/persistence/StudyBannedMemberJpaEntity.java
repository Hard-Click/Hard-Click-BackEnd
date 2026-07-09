package com.wanted.backend.domain.study.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(name = "study_banned_member")
@Getter
public class StudyBannedMemberJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "study_banned_member_id")
    private Long id;

    @Column(name = "study_id", nullable = false)
    private Long studyId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "banned_at", nullable = false)
    private LocalDateTime bannedAt;

    protected StudyBannedMemberJpaEntity() {}

    public StudyBannedMemberJpaEntity(Long studyId, Long memberId, LocalDateTime bannedAt) {
        this.studyId = studyId;
        this.memberId = memberId;
        this.bannedAt = bannedAt;
    }
}
