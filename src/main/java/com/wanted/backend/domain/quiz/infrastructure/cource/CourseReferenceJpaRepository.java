package com.wanted.backend.domain.quiz.infrastructure.cource;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

// 관리자 강의 목록의 선택적 필터(subject/instructor/course/keyword)는 Criteria 기반 Specification으로 조합한다.
public interface CourseReferenceJpaRepository extends JpaRepository<CourseReferenceJpaEntity, Long>,
        JpaSpecificationExecutor<CourseReferenceJpaEntity> {
}
