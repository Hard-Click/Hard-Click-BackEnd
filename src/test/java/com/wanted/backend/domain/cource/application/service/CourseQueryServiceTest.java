package com.wanted.backend.domain.cource.application.service;

import com.wanted.backend.domain.cource.application.dto.CourseDetailResult;
import com.wanted.backend.domain.cource.application.port.CourseLearningPolicyPort;
import com.wanted.backend.domain.cource.application.port.EnrollmentStatsPort;
import com.wanted.backend.domain.cource.application.port.InstructorQueryPort;
import com.wanted.backend.domain.cource.application.port.InstructorStatsPort;
import com.wanted.backend.domain.cource.application.port.ReviewStatsPort;
import com.wanted.backend.domain.cource.domain.model.Course;
import com.wanted.backend.domain.cource.domain.model.CourseStatus;
import com.wanted.backend.domain.cource.domain.model.PriceType;
import com.wanted.backend.domain.cource.domain.repository.CourseRepository;
import com.wanted.backend.global.config.S3UrlPresigner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 강의 상세 조회의 학습 정책(recommendedWeeks·dailyMaxMinutes) 노출 회귀 방지.
 * 등록 때 저장되는 두 값이 상세 응답에 다시 내려와야 강사 수정 화면이 프리필할 수 있다.
 * (정책상 학생 포함 전체에 노출 — 소유 강사 게이팅 없음)
 */
class CourseQueryServiceTest {

    private static final Long COURSE_ID = 88L;
    private static final Long AUTHOR_ID = 1L;

    private CourseRepository courseRepository;
    private CourseLearningPolicyPort courseLearningPolicyPort;
    private CourseQueryService service;

    @BeforeEach
    void setUp() {
        courseRepository = mock(CourseRepository.class);
        InstructorQueryPort instructorQueryPort = mock(InstructorQueryPort.class);
        ReviewStatsPort reviewStatsPort = mock(ReviewStatsPort.class);
        EnrollmentStatsPort enrollmentStatsPort = mock(EnrollmentStatsPort.class);
        InstructorStatsPort instructorStatsPort = mock(InstructorStatsPort.class);
        courseLearningPolicyPort = mock(CourseLearningPolicyPort.class);
        S3UrlPresigner s3UrlPresigner = mock(S3UrlPresigner.class);

        // 상세 조립에 필요한 협력자 스텁(본 테스트 관심사가 아니므로 lenient).
        lenient().when(instructorQueryPort.findNamesByIds(any())).thenReturn(Map.of(AUTHOR_ID, "박강사"));
        lenient().when(instructorQueryPort.findProfileById(anyLong()))
                .thenReturn(new InstructorQueryPort.InstructorProfile("한줄", "소개", "경력"));
        lenient().when(s3UrlPresigner.publicUrl(any())).thenReturn(null);

        service = new CourseQueryService(
                courseRepository, instructorQueryPort, reviewStatsPort,
                enrollmentStatsPort, instructorStatsPort, courseLearningPolicyPort, s3UrlPresigner);
    }

    private Course publishedCourse() {
        return Course.restore(COURSE_ID, AUTHOR_ID, "제목", "과목", "설명", null,
                PriceType.FREE, 0, CourseStatus.PUBLISHED, List.of(), Instant.now(),
                List.of(), List.of(), List.of(), "BEGINNER");
    }

    @Test
    @DisplayName("정책이 저장돼 있으면 상세 응답에 권장 완강 주수·하루 강도 상한이 포함된다")
    void detailIncludesLearningPolicyWhenPresent() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(publishedCourse()));
        when(courseLearningPolicyPort.find(COURSE_ID))
                .thenReturn(Optional.of(new CourseLearningPolicyPort.LearningPolicy(12, 90)));

        CourseDetailResult result = service.getDetail(COURSE_ID, AUTHOR_ID);

        assertThat(result.recommendedWeeks()).isEqualTo(12);
        assertThat(result.dailyMaxMinutes()).isEqualTo(90);
    }

    @Test
    @DisplayName("정책 레코드가 없는 구 강의는 두 필드가 null로 안전하게 내려간다")
    void detailReturnsNullPolicyFieldsWhenAbsent() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(publishedCourse()));
        when(courseLearningPolicyPort.find(COURSE_ID)).thenReturn(Optional.empty());

        CourseDetailResult result = service.getDetail(COURSE_ID, AUTHOR_ID);

        assertThat(result.recommendedWeeks()).isNull();
        assertThat(result.dailyMaxMinutes()).isNull();
    }

    @Test
    @DisplayName("소유 강사가 아닌 요청(학생·비로그인)에도 정책값이 그대로 노출된다")
    void detailExposesPolicyToNonOwners() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(publishedCourse()));
        when(courseLearningPolicyPort.find(COURSE_ID))
                .thenReturn(Optional.of(new CourseLearningPolicyPort.LearningPolicy(8, 60)));

        // requesterId = null(비로그인) 및 다른 회원 → 모두 값이 내려가야 함
        assertThat(service.getDetail(COURSE_ID, null).recommendedWeeks()).isEqualTo(8);
        assertThat(service.getDetail(COURSE_ID, 999L).dailyMaxMinutes()).isEqualTo(60);
    }
}
