package com.wanted.backend.domain.quiz.application.port;

import java.util.Collection;
import java.util.Map;

/**
 * 강의별 활성 수강생 수 조회 아웃바운드 포트.
 * 활성 = IN_PROGRESS/COMPLETED + 미만료 (CourseStudentPort와 동일한 의미).
 */
public interface CourseEnrollmentCountPort {

    // 강의ID → 활성 수강생 수. 수강생이 없는 강의는 결과 맵에서 생략될 수 있다(호출부에서 0 처리).
    Map<Long, Integer> countActiveStudentsByCourseIds(Collection<Long> courseIds);
}
