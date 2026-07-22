package com.wanted.backend.domain.enrollment_management.infrastructure.persistence;

import com.wanted.backend.domain.enrollment_management.application.port.MyEnrolledCourseQueryPort.MyEnrolledCourseData;
import com.wanted.backend.domain.enrollment_management.domain.model.EnrollmentStatus;
import com.wanted.backend.domain.enrollment_management.infrastructure.course.CourseReferenceEntity;
import com.wanted.backend.domain.enrollment_management.infrastructure.course.CourseReferenceRepository;
import com.wanted.backend.domain.enrollment_management.infrastructure.lesson.EnrolledLessonReferenceEntity;
import com.wanted.backend.domain.enrollment_management.infrastructure.lesson.EnrolledLessonReferenceRepository;
import com.wanted.backend.domain.enrollment_management.infrastructure.progress.VideoProgressReferenceEntity;
import com.wanted.backend.domain.enrollment_management.infrastructure.progress.VideoProgressReferenceRepository;
import com.wanted.backend.domain.enrollment_management.infrastructure.section.EnrolledCourseSectionReferenceEntity;
import com.wanted.backend.domain.enrollment_management.infrastructure.section.EnrolledCourseSectionReferenceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@EntityScan(basePackageClasses = {
        EnrollmentJpaEntity.class,
        CourseReferenceEntity.class,
        EnrolledCourseSectionReferenceEntity.class,
        EnrolledLessonReferenceEntity.class,
        VideoProgressReferenceEntity.class
})
@EnableJpaRepositories(basePackageClasses = {
        SpringDataEnrollmentRepository.class,
        CourseReferenceRepository.class,
        EnrolledCourseSectionReferenceRepository.class,
        EnrolledLessonReferenceRepository.class,
        VideoProgressReferenceRepository.class
})
@Import({
        EnrollmentRepositoryAdapter.class,
        MyEnrolledCourseQueryAdapter.class
})
@Sql(scripts = {
        "/sql/enrollment_management_adapter_schema.sql",
        "/sql/enrollment_management_adapter_data.sql"
})
class MyEnrolledCourseQueryAdapterTest {

    @Autowired
    private MyEnrolledCourseQueryAdapter queryAdapter;

    @Test
    void 진도가_있으면_lastVideoId는_가장_최근_진도의_레슨이다() {
        List<MyEnrolledCourseData> result = queryAdapter.findByMemberId(1L);

        assertThat(result).hasSize(1);
        MyEnrolledCourseData data = result.get(0);
        assertThat(data.courseId()).isEqualTo(20L);
        assertThat(data.courseTitle()).isEqualTo("Spring Course");
        // 섹션 id(50, 31)와 레슨 id(10, 15, 70)가 order_index와 일부러 어긋나게 들어가 있다 —
        // id가 아니라 order_index(섹션 -> 레슨 순)로 정렬되는지를 totalLessonCount로 간접 확인.
        assertThat(data.totalLessonCount()).isEqualTo(3);
        assertThat(data.completedLessonCount()).isEqualTo(1);
        assertThat(data.lastVideoId()).isEqualTo(10L);
        assertThat(data.lastPositionSeconds()).isEqualTo(42);
        assertThat(data.enrollmentStatus()).isEqualTo(EnrollmentStatus.IN_PROGRESS);
    }

    @Test
    void 진도가_없으면_lastVideoId는_섹션_레슨_순서상_첫_레슨이다() {
        List<MyEnrolledCourseData> result = queryAdapter.findByMemberId(2L);

        assertThat(result).hasSize(1);
        MyEnrolledCourseData data = result.get(0);
        // order_index 기준 첫 섹션(id=31, order_index=0)의 첫 레슨(id=15, order_index=0) —
        // 둘 다 숫자상 id는 더 작은 값(10/31...)이 아니라 order_index가 정렬을 결정함을 검증한다.
        assertThat(data.lastVideoId()).isEqualTo(15L);
        assertThat(data.completedLessonCount()).isZero();
        assertThat(data.lastPositionSeconds()).isZero();
        assertThat(data.lastStudiedAt()).isNull();
    }

    @Test
    void 마이코스_목록_대상이_아닌_상태의_수강은_결과에서_제외된다() {
        List<MyEnrolledCourseData> result = queryAdapter.findByMemberId(3L);

        assertThat(result).isEmpty();
    }

    @Test
    void 수강한_강의가_없으면_빈_목록을_반환한다() {
        List<MyEnrolledCourseData> result = queryAdapter.findByMemberId(999L);

        assertThat(result).isEmpty();
    }

    /**
     * 수강 상태는 정상(IN_PROGRESS)이지만 강의가 소프트 삭제(status=DELETED)된 경우,
     * 목록에서 제외돼야 한다 — 상세는 삭제 강의에 404를 내므로 목록에만 남으면 클릭 시 불일치가 난다.
     */
    @Test
    void 소프트_삭제된_강의의_수강은_목록에서_제외된다() {
        List<MyEnrolledCourseData> result = queryAdapter.findByMemberId(4L);

        assertThat(result).isEmpty();
    }

    /**
     * 강의가 비공개(status=DRAFT)면 수강 중이어도 목록에서 제외돼야 한다 — PUBLISHED 만 노출.
     * (강사가 게시 중단한 강의를 학생 수강 목록에 남기지 않는다.)
     */
    @Test
    void 비공개_DRAFT_강의의_수강은_목록에서_제외된다() {
        List<MyEnrolledCourseData> result = queryAdapter.findByMemberId(5L);

        assertThat(result).isEmpty();
    }
}
