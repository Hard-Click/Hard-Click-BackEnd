package com.wanted.backend.domain.enrollment_management.infrastructure.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CourseReferenceRepository extends JpaRepository<CourseReferenceEntity, Long> {

    List<CourseReferenceEntity> findByIdIn(Collection<Long> courseIds);

    // 소프트 삭제(status=DELETED)된 강의는 제외하고 조회한다.
    List<CourseReferenceEntity> findByIdInAndStatusNot(Collection<Long> courseIds, String status);
}
