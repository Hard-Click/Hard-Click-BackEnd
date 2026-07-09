package com.wanted.backend.domain.quiz.application.port;

import java.util.List;

/**
 * 강의의 활성 수강생 목록을 조회하는 아웃바운드 포트.
 * 강사 퀴즈 점수 현황 통계에서 전체 인원/미응시자 집계에 사용한다.
 */
public interface CourseStudentPort {

    List<CourseStudent> findActiveStudents(Long courseId);

    record CourseStudent(Long memberId, String username, String name) {}
}
