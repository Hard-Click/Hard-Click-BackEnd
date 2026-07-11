package com.wanted.backend.domain.quiz.infrastructure.cource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

// cource 도메인 course 테이블의 읽기 전용 참조 (Port + ReferenceEntity 패턴).
// 강의명 표시(CourseTitle) + 관리자 강의 목록(제목/공개여부/강사/등록일 + 필터)에 사용한다.
// status는 cource 도메인 enum에 의존하지 않도록 문자열('DRAFT'/'PUBLISHED'/'DELETED')로 읽는다.
@Entity
@Immutable
@Table(name = "course")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseReferenceJpaEntity {

    @Id
    @Column(name = "course_id")
    private Long id;

    @Column(name = "title")
    private String title;

    @Column(name = "author_id")
    private Long authorId;

    @Column(name = "subject")
    private String subject;

    @Column(name = "status")
    private String status;

    @Column(name = "created_at")
    private Instant createdAt;
}
