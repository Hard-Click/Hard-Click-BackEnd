package com.wanted.backend.domain.quiz.infrastructure.cource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

// cource 도메인 course_section 테이블의 읽기 전용 참조 (Port + ReferenceEntity 패턴, cart의
// CartCourseJpaEntity와 동일한 방식) — 섹션 존재/소속 확인과 섹션명 표시에 사용.
@Entity
@Immutable
@Table(name = "course_section")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseSectionReferenceJpaEntity {

    @Id
    private Long id;

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "title")
    private String title;
}
