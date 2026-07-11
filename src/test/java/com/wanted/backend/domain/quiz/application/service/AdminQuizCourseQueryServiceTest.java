package com.wanted.backend.domain.quiz.application.service;

import com.wanted.backend.domain.quiz.application.port.AdminCourseListPort;
import com.wanted.backend.domain.quiz.application.port.CourseEnrollmentCountPort;
import com.wanted.backend.domain.quiz.application.port.MemberNamePort;
import com.wanted.backend.domain.quiz.application.result.AdminQuizCourse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminQuizCourseQueryServiceTest {

    private AdminCourseListPort adminCourseListPort;
    private CourseEnrollmentCountPort courseEnrollmentCountPort;
    private MemberNamePort memberNamePort;
    private AdminQuizCourseQueryService service;

    private static final Instant T1 = Instant.parse("2026-05-10T00:00:00Z");
    private static final Instant T2 = Instant.parse("2026-04-22T00:00:00Z");

    @BeforeEach
    void setUp() {
        adminCourseListPort = mock(AdminCourseListPort.class);
        courseEnrollmentCountPort = mock(CourseEnrollmentCountPort.class);
        memberNamePort = mock(MemberNamePort.class);
        service = new AdminQuizCourseQueryService(adminCourseListPort, courseEnrollmentCountPort, memberNamePort);
    }

    @Test
    void assemblesCoursesWithStudentCountsAndInstructorNames() {
        when(adminCourseListPort.findCourses(any(), any(), any(), any())).thenReturn(List.of(
                new AdminCourseListPort.AdminCourse(1L, "React 완벽 가이드", true, 10L, T1),
                new AdminCourseListPort.AdminCourse(2L, "수1 정복하기", false, 20L, T2)));
        when(courseEnrollmentCountPort.countActiveStudentsByCourseIds(anyCollection()))
                .thenReturn(Map.of(1L, 89, 2L, 124));
        when(memberNamePort.findNamesByIds(anyCollection()))
                .thenReturn(Map.of(10L, "안현", 20L, "김종호"));

        List<AdminQuizCourse> result = service.getCourses(null, null, null, null);

        assertThat(result).hasSize(2);
        AdminQuizCourse first = result.get(0);
        assertThat(first.courseId()).isEqualTo(1L);
        assertThat(first.courseTitle()).isEqualTo("React 완벽 가이드");
        assertThat(first.visible()).isTrue();
        assertThat(first.studentCount()).isEqualTo(89);
        assertThat(first.instructorName()).isEqualTo("안현");
        assertThat(first.registeredAt()).isEqualTo(T1);
        assertThat(result.get(1).studentCount()).isEqualTo(124);
        assertThat(result.get(1).instructorName()).isEqualTo("김종호");
        assertThat(result.get(1).visible()).isFalse();
    }

    @Test
    void defaultsMissingStudentCountToZeroAndMissingInstructorNameToUnknown() {
        when(adminCourseListPort.findCourses(any(), any(), any(), any())).thenReturn(List.of(
                new AdminCourseListPort.AdminCourse(1L, "React", true, 10L, T1)));
        when(courseEnrollmentCountPort.countActiveStudentsByCourseIds(anyCollection())).thenReturn(Map.of());
        when(memberNamePort.findNamesByIds(anyCollection())).thenReturn(Map.of());

        List<AdminQuizCourse> result = service.getCourses(null, null, null, null);

        assertThat(result.get(0).studentCount()).isZero();
        assertThat(result.get(0).instructorName()).isEqualTo("알 수 없음");
    }

    @Test
    void returnsEmptyAndSkipsJoinsWhenNoCoursesMatch() {
        when(adminCourseListPort.findCourses(any(), any(), any(), any())).thenReturn(List.of());

        assertThat(service.getCourses("수학1", 5L, null, "React")).isEmpty();

        verify(courseEnrollmentCountPort, never()).countActiveStudentsByCourseIds(anyCollection());
        verify(memberNamePort, never()).findNamesByIds(anyCollection());
    }

    @Test
    void passesFiltersThroughToTheCourseListPort() {
        when(adminCourseListPort.findCourses("수학1", 5L, 7L, "미적분")).thenReturn(List.of());

        service.getCourses("수학1", 5L, 7L, "미적분");

        verify(adminCourseListPort).findCourses("수학1", 5L, 7L, "미적분");
    }
}
