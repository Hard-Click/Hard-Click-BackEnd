package com.wanted.backend.domain.quiz.application.service;

import com.wanted.backend.domain.quiz.application.port.AdminCourseListPort;
import com.wanted.backend.domain.quiz.application.port.CourseEnrollmentCountPort;
import com.wanted.backend.domain.quiz.application.port.MemberNamePort;
import com.wanted.backend.domain.quiz.application.result.AdminQuizCourse;
import com.wanted.backend.domain.quiz.application.usecase.AdminQuizCourseQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminQuizCourseQueryService implements AdminQuizCourseQueryUseCase {

    private static final String UNKNOWN_INSTRUCTOR = "알 수 없음";

    private final AdminCourseListPort adminCourseListPort;
    private final CourseEnrollmentCountPort courseEnrollmentCountPort;
    private final MemberNamePort memberNamePort;

    @Override
    public List<AdminQuizCourse> getCourses(String subject, Long instructorId, Long courseId, String keyword) {
        List<AdminCourseListPort.AdminCourse> courses =
                adminCourseListPort.findCourses(subject, instructorId, courseId, keyword);
        if (courses.isEmpty()) {
            return List.of();
        }

        // 수강생 수·강사명을 각각 배치로 조회해 조인 (N+1 방지)
        Map<Long, Integer> studentCounts = courseEnrollmentCountPort.countActiveStudentsByCourseIds(
                courses.stream().map(AdminCourseListPort.AdminCourse::courseId).toList());
        Map<Long, String> instructorNames = memberNamePort.findNamesByIds(
                courses.stream().map(AdminCourseListPort.AdminCourse::instructorId).distinct().toList());

        return courses.stream()
                .map(c -> new AdminQuizCourse(
                        c.courseId(),
                        c.title(),
                        c.visible(),
                        studentCounts.getOrDefault(c.courseId(), 0),
                        instructorNames.getOrDefault(c.instructorId(), UNKNOWN_INSTRUCTOR),
                        c.registeredAt()))
                .toList();
    }
}
