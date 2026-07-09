package com.wanted.backend.domain.study.domain.model;

import java.time.LocalDateTime;

public class StudyBannedMember {

    private Long id;
    private Long studyId;
    private Long memberId;
    private LocalDateTime bannedAt;

    private StudyBannedMember(Long id, Long studyId, Long memberId, LocalDateTime bannedAt) {
        this.id = id;
        this.studyId = studyId;
        this.memberId = memberId;
        this.bannedAt = bannedAt;
    }

    public static StudyBannedMember create(Long studyId, Long memberId) {
        return new StudyBannedMember(null, studyId, memberId, LocalDateTime.now());
    }

    public static StudyBannedMember restore(Long id, Long studyId, Long memberId, LocalDateTime bannedAt) {
        return new StudyBannedMember(id, studyId, memberId, bannedAt);
    }

    public Long getId() { return id; }
    public Long getStudyId() { return studyId; }
    public Long getMemberId() { return memberId; }
    public LocalDateTime getBannedAt() { return bannedAt; }
}
