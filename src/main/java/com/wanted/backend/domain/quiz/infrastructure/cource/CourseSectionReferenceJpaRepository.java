package com.wanted.backend.domain.quiz.infrastructure.cource;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseSectionReferenceJpaRepository extends JpaRepository<CourseSectionReferenceJpaEntity, Long> {

    boolean existsByIdAndCourseId(Long id, Long courseId);
}
