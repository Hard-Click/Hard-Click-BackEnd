package com.wanted.backend.domain.quiz.infrastructure.cource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

// cource 도메인 course 테이블의 읽기 전용 참조 (Port + ReferenceEntity 패턴) — 강의명 표시에 사용.
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
}
