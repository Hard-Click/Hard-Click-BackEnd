package com.wanted.backend.domain.quiz.infrastructure.enrollment;

import com.wanted.backend.domain.enrollment_management.domain.model.EnrollmentStatus;
import com.wanted.backend.domain.quiz.application.port.CourseStudentPort;
import com.wanted.backend.domain.quiz.infrastructure.member.QuizMemberReferenceJpaEntity;
import com.wanted.backend.domain.quiz.infrastructure.member.QuizMemberReferenceJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseStudentAdapter implements CourseStudentPort {

    // 통계 대상 수강생: 응시 자격과 동일하게 IN_PROGRESS/COMPLETED만 (제출 수강 검증과 일관).
    private static final Set<EnrollmentStatus> ACTIVE_ENROLLMENT_STATUSES =
            EnumSet.of(EnrollmentStatus.IN_PROGRESS, EnrollmentStatus.COMPLETED);

    private final QuizEnrollmentJpaRepository enrollmentRepository;
    private final QuizMemberReferenceJpaRepository memberRepository;

    @Override
    public List<CourseStudent> findActiveStudents(Long courseId) {
        // 만료 없음 + 미만료(만료일 미래) 두 갈래를 합쳐 활성 수강생 memberId를 모은다.
        Set<Long> memberIds = new LinkedHashSet<>();
        enrollmentRepository.findByCourseIdAndStatusInAndExpiredAtIsNull(courseId, ACTIVE_ENROLLMENT_STATUSES)
                .forEach(e -> memberIds.add(e.getMemberId()));
        enrollmentRepository.findByCourseIdAndStatusInAndExpiredAtGreaterThanEqual(
                        courseId, ACTIVE_ENROLLMENT_STATUSES, LocalDateTime.now())
                .forEach(e -> memberIds.add(e.getMemberId()));

        if (memberIds.isEmpty()) {
            return List.of();
        }

        Map<Long, QuizMemberReferenceJpaEntity> memberById = memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(QuizMemberReferenceJpaEntity::getId, Function.identity()));

        return memberIds.stream()
                .map(memberById::get)
                .filter(java.util.Objects::nonNull)
                .map(m -> new CourseStudent(m.getId(), m.getUsername(), m.getName()))
                .toList();
    }
}
