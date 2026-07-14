package com.wanted.backend.domain.quiz.infrastructure;

import com.wanted.backend.domain.quiz.application.port.AdminCourseListPort;
import com.wanted.backend.domain.quiz.infrastructure.cource.AdminCourseListAdapter;
import com.wanted.backend.domain.quiz.infrastructure.cource.CourseReferenceJpaEntity;
import com.wanted.backend.domain.quiz.infrastructure.cource.CourseReferenceJpaRepository;
import com.wanted.backend.domain.quiz.infrastructure.enrollment.CourseEnrollmentCountAdapter;
import com.wanted.backend.domain.quiz.infrastructure.enrollment.QuizEnrollmentJpaRepository;
import com.wanted.backend.domain.quiz.infrastructure.enrollment.QuizEnrollmentReferenceJpaEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 관리자 강의 목록/수강생수 어댑터의 실제 쿼리 동작을 시딩 데이터로 검증한다.
 * (전체 엔티티 DDL 생성/두-엔티티 충돌을 피하려 ddl-auto=none + @EntityScan 스코프 + @Sql 최소 스키마 사용.)
 */
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false"
})
@EntityScan(basePackageClasses = {CourseReferenceJpaEntity.class, QuizEnrollmentReferenceJpaEntity.class})
@EnableJpaRepositories(basePackageClasses = {CourseReferenceJpaRepository.class, QuizEnrollmentJpaRepository.class})
@Import({AdminCourseListAdapter.class, CourseEnrollmentCountAdapter.class})
@Sql(scripts = {"/sql/admin_course_list_schema.sql", "/sql/admin_course_list_data.sql"},
        config = @SqlConfig(encoding = "UTF-8"))
class AdminCourseListAdapterTest {

    @Autowired
    private AdminCourseListAdapter adminCourseListAdapter;

    @Autowired
    private CourseEnrollmentCountAdapter courseEnrollmentCountAdapter;

    @Test
    void excludesDeletedCoursesAndSortsByCreatedAtDesc() {
        // DELETED(3) 제외 → 1,2,4. createdAt desc: 2(05-12) > 1(05-10) > 4(04-22)
        assertThat(adminCourseListAdapter.findCourses(null, null, null, null))
                .extracting(AdminCourseListPort.AdminCourse::courseId)
                .containsExactly(2L, 1L, 4L);
    }

    @Test
    void derivesVisibleFromPublishedStatus() {
        Map<Long, AdminCourseListPort.AdminCourse> byId = adminCourseListAdapter.findCourses(null, null, null, null)
                .stream().collect(java.util.stream.Collectors.toMap(AdminCourseListPort.AdminCourse::courseId, c -> c));

        assertThat(byId.get(1L).visible()).isTrue();   // PUBLISHED
        assertThat(byId.get(2L).visible()).isFalse();  // DRAFT
    }

    @Test
    void filtersBySubjectInstructorCourseIdAndKeyword() {
        assertThat(adminCourseListAdapter.findCourses("수학1", null, null, null))
                .extracting(AdminCourseListPort.AdminCourse::courseId).containsExactly(4L);
        assertThat(adminCourseListAdapter.findCourses(null, 20L, null, null))
                .extracting(AdminCourseListPort.AdminCourse::courseId).containsExactly(4L);
        assertThat(adminCourseListAdapter.findCourses(null, null, 1L, null))
                .extracting(AdminCourseListPort.AdminCourse::courseId).containsExactly(1L);
        // keyword 대소문자 무시 부분검색: "react" → 1,2 (정렬 desc: 2,1)
        assertThat(adminCourseListAdapter.findCourses(null, null, null, "react"))
                .extracting(AdminCourseListPort.AdminCourse::courseId).containsExactly(2L, 1L);
    }

    @Test
    void countsDistinctActiveStudentsPerCourse() {
        Map<Long, Integer> counts = courseEnrollmentCountAdapter.countActiveStudentsByCourseIds(List.of(1L, 4L));

        // course 1: member 100(만료null)/101(미래만료)/102(활성2건 dedup) = 3, 103(EXPIRED)·104(과거만료) 제외
        assertThat(counts.get(1L)).isEqualTo(3);
        assertThat(counts.get(4L)).isEqualTo(1);
    }
}
