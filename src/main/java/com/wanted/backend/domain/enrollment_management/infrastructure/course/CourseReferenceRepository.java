package com.wanted.backend.domain.enrollment_management.infrastructure.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CourseReferenceRepository extends JpaRepository<CourseReferenceEntity, Long> {

    List<CourseReferenceEntity> findByIdIn(Collection<Long> courseIds);

    // 수강 목록에 노출할 상태(PUBLISHED)만 조회한다 — 삭제(DELETED)·비공개(DRAFT)는 모두 제외.
    // 긍정형 단일 상태라 (course_id, status) 인덱스 활용에 유리하다.
    List<CourseReferenceEntity> findByIdInAndStatus(Collection<Long> courseIds, String status);
}
