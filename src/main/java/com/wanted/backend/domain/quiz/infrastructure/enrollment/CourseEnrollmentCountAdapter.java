package com.wanted.backend.domain.quiz.infrastructure.enrollment;

import com.wanted.backend.domain.enrollment_management.domain.model.EnrollmentStatus;
import com.wanted.backend.domain.quiz.application.port.CourseEnrollmentCountPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseEnrollmentCountAdapter implements CourseEnrollmentCountPort {

    // 활성 수강생: 응시 자격과 동일하게 IN_PROGRESS/COMPLETED (CourseStudentAdapter와 일관).
    private static final Set<EnrollmentStatus> ACTIVE_ENROLLMENT_STATUSES =
            EnumSet.of(EnrollmentStatus.IN_PROGRESS, EnrollmentStatus.COMPLETED);

    private final QuizEnrollmentJpaRepository enrollmentRepository;

    @Override
    public Map<Long, Integer> countActiveStudentsByCourseIds(Collection<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return Map.of();
        }

        // 만료 없음 + 미만료(만료일 미래) 두 갈래를 배치(각 1쿼리)로 가져와 강의별 고유 memberId를 센다.
        Map<Long, Set<Long>> membersByCourse = new HashMap<>();
        enrollmentRepository.findByCourseIdInAndStatusInAndExpiredAtIsNull(courseIds, ACTIVE_ENROLLMENT_STATUSES)
                .forEach(e -> membersByCourse.computeIfAbsent(e.getCourseId(), k -> new HashSet<>()).add(e.getMemberId()));
        enrollmentRepository.findByCourseIdInAndStatusInAndExpiredAtGreaterThanEqual(
                        courseIds, ACTIVE_ENROLLMENT_STATUSES, LocalDateTime.now())
                .forEach(e -> membersByCourse.computeIfAbsent(e.getCourseId(), k -> new HashSet<>()).add(e.getMemberId()));

        Map<Long, Integer> counts = new HashMap<>();
        membersByCourse.forEach((courseId, members) -> counts.put(courseId, members.size()));
        return counts;
    }
}
