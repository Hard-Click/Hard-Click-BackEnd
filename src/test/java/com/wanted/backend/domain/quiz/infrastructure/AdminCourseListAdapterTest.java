package com.wanted.backend.domain.quiz.infrastructure;

import com.wanted.backend.domain.quiz.infrastructure.cource.AdminCourseListAdapter;
import com.wanted.backend.domain.quiz.infrastructure.enrollment.CourseEnrollmentCountAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 관리자 강의 목록 어댑터의 실제 쿼리 배선을 검증한다:
 * Specification(과목/강사/강의/검색어 + DELETED 제외)과 수강생 수 배치 파생쿼리가 매핑 오류 없이 실행되는지.
 * (참조 엔티티가 읽기 전용이라 데이터 시딩 대신 쿼리 실행 자체를 검증한다.)
 */
@DataJpaTest(properties = {
        "spring.jpa.database=H2",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@ActiveProfiles("test")
@Import({AdminCourseListAdapter.class, CourseEnrollmentCountAdapter.class})
class AdminCourseListAdapterTest {

    @Autowired
    private AdminCourseListAdapter adminCourseListAdapter;

    @Autowired
    private CourseEnrollmentCountAdapter courseEnrollmentCountAdapter;

    @Test
    void courseListSpecificationExecutesWithAndWithoutFilters() {
        assertThat(adminCourseListAdapter.findCourses(null, null, null, null)).isEmpty();
        assertThat(adminCourseListAdapter.findCourses("수학1", 5L, 7L, "React")).isEmpty();
    }

    @Test
    void activeStudentCountBatchQueryExecutes() {
        assertThat(courseEnrollmentCountAdapter.countActiveStudentsByCourseIds(List.of(1L, 2L))).isEmpty();
        assertThat(courseEnrollmentCountAdapter.countActiveStudentsByCourseIds(List.of())).isEmpty();
    }
}
