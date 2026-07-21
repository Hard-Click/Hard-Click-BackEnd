package com.wanted.backend.domain.community.infrastructure.study;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/**
 * community가 전체 피드에 스터디를 합치기 위해 study 테이블을 읽는 참조 read-model.
 *
 * <p>enrollment_management의 CourseReferenceEntity 와 동일한 패턴 — 도메인 코드 의존 대신
 * 상대 테이블만 읽는다. status 는 study 도메인 enum(ACTIVE/FULL/DISSOLVED)이지만 여기선
 * 문자열로만 다룬다(도메인 결합 회피). 모집 마감 여부는 status != ACTIVE.
 */
@Entity(name = "CommunityStudyReference")
@Getter
@Immutable
@Table(name = "study")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyReferenceEntity {

    @Id
    @Column(name = "study_id")
    private Long id;

    @Column(name = "host_id", nullable = false)
    private Long hostId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String subject;

    @Column(name = "current_count", nullable = false)
    private int currentCount;

    @Column(name = "max_count", nullable = false)
    private int maxCount;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
