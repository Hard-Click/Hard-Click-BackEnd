package com.wanted.backend.domain.quiz.application.usecase;

import com.wanted.backend.domain.quiz.application.result.AdminQuizCourse;

import java.util.List;

/**
 * 관리자 퀴즈 관리 강의 목록 조회 (ADMIN 전용, 인가는 컨트롤러 @PreAuthorize가 보장).
 */
public interface AdminQuizCourseQueryUseCase {

    List<AdminQuizCourse> getCourses(String subject, Long instructorId, Long courseId, String keyword);
}
