package com.wanted.backend.domain.quiz.infrastructure.enrollment;

import com.wanted.backend.domain.enrollment_management.domain.model.EnrollmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

// enrollment 테이블의 읽기 전용 참조 (Port + ReferenceEntity 패턴) — 수강 여부 확인용.
// 클래스명은 도메인별로 고유해야 한다 (Hibernate 엔티티명 충돌 방지 — learning_activity의
// EnrollmentReferenceJpaEntity와 단순명이 겹치면 DuplicateMappingException 발생).
@Entity
@Getter
@Immutable
@Table(name = "enrollment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizEnrollmentReferenceJpaEntity {

    @Id
    @Column(name = "enrollment_id")
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;
}
