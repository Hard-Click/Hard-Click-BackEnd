package com.wanted.backend.domain.study.domain.model;

import java.time.LocalDateTime;

public class StudyParticipant {

    private Long id;
    private Long studyId;
    private Long memberId;
    private LocalDateTime joinedAt;

    private StudyParticipant(Long id, Long studyId, Long memberId, LocalDateTime joinedAt) {
        this.id = id;
        this.studyId = studyId;
        this.memberId = memberId;
        this.joinedAt = joinedAt;
    }

    public static StudyParticipant create(Long studyId, Long memberId) {
        return new StudyParticipant(null, studyId, memberId, LocalDateTime.now());
    }

    public static StudyParticipant restore(Long id, Long studyId, Long memberId, LocalDateTime joinedAt) {
        return new StudyParticipant(id, studyId, memberId, joinedAt);
    }

    public Long getId() { return id; }
    public Long getStudyId() { return studyId; }
    public Long getMemberId() { return memberId; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
}
