package com.wanted.backend.domain.quiz.application.port;

import java.time.Instant;
import java.util.List;

/**
 * 관리자 퀴즈 관리 화면용 강의 목록 조회 아웃바운드 포트.
 * cource 도메인 course 테이블을 필터(과목/강사/강의/검색어)로 조회한다. soft-delete(DELETED)된 강의는 제외한다.
 */
public interface AdminCourseListPort {

    List<AdminCourse> findCourses(String subject, Long instructorId, Long courseId, String keyword);

    record AdminCourse(
            Long courseId,
            String title,
            boolean visible,        // 공개 여부 (PUBLISHED = true)
            Long instructorId,
            Instant registeredAt
    ) {}
}
