package com.wanted.backend.domain.enrollment_management.application.service;

import com.wanted.backend.domain.enrollment_management.application.port.MyEnrolledCourseQueryPort;
import com.wanted.backend.domain.enrollment_management.application.usecase.GetMyEnrolledCourseUseCase.MyEnrolledCourseView;
import com.wanted.backend.domain.enrollment_management.domain.model.EnrollmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MyEnrolledCourseServiceTest {

    private MyEnrolledCourseQueryPort queryPort;
    private MyEnrolledCourseService service;

    @BeforeEach
    void setUp() {
        queryPort = mock(MyEnrolledCourseQueryPort.class);
        service = new MyEnrolledCourseService(queryPort);
    }

    @Test
    void 내_수강_강의_목록과_진도율을_반환한다() {
        LocalDateTime lastStudiedAt = LocalDateTime.of(2026, 5, 27, 10, 30);
        when(queryPort.findByMemberId(1L)).thenReturn(List.of(
                new MyEnrolledCourseQueryPort.MyEnrolledCourseData(
                        20L,
                        "Spring Course",
                        "https://example.com/thumb.png",
                        1,
                        4,
                        lastStudiedAt,
                        10L,
                        142,
                        EnrollmentStatus.IN_PROGRESS
                )
        ));

        List<MyEnrolledCourseView> result = service.handle(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).courseId()).isEqualTo(20L);
        assertThat(result.get(0).courseTitle()).isEqualTo("Spring Course");
        assertThat(result.get(0).progressRate()).isEqualTo(25);
        assertThat(result.get(0).lastVideoId()).isEqualTo(10L);
        assertThat(result.get(0).lastPositionSeconds()).isEqualTo(142);
        assertThat(result.get(0).lastStudiedAt()).isEqualTo(lastStudiedAt);
    }

    @Test
    void 최근_학습일시_내림차순으로_정렬하고_미학습_강의는_마지막에_둔다() {
        LocalDateTime older = LocalDateTime.of(2026, 5, 26, 10, 0);
        LocalDateTime newer = LocalDateTime.of(2026, 5, 27, 10, 0);
        when(queryPort.findByMemberId(1L)).thenReturn(List.of(
                data(1L, older),
                data(2L, null),
                data(3L, newer)
        ));

        List<MyEnrolledCourseView> result = service.handle(1L);

        assertThat(result).extracting(MyEnrolledCourseView::courseId)
                .containsExactly(3L, 1L, 2L);
    }

    @Test
    void 강의에_영상이_없으면_진도율은_0이다() {
        when(queryPort.findByMemberId(1L)).thenReturn(List.of(
                new MyEnrolledCourseQueryPort.MyEnrolledCourseData(
                        20L,
                        "Empty Course",
                        null,
                        0,
                        0,
                        null,
                        null,
                        0,
                        EnrollmentStatus.IN_PROGRESS
                )
        ));

        List<MyEnrolledCourseView> result = service.handle(1L);

        assertThat(result.get(0).progressRate()).isZero();
    }

    @Test
    void 진도율이_100퍼센트인_강의는_수강중_목록에서_제외된다() {
        when(queryPort.findByMemberId(1L)).thenReturn(List.of(
                new MyEnrolledCourseQueryPort.MyEnrolledCourseData(
                        10L, "완료 강의", null,
                        3, 3,
                        LocalDateTime.of(2026, 5, 27, 10, 0),
                        30L, 0, EnrollmentStatus.IN_PROGRESS
                ),
                new MyEnrolledCourseQueryPort.MyEnrolledCourseData(
                        20L, "진행중 강의", null,
                        1, 3,
                        LocalDateTime.of(2026, 5, 26, 10, 0),
                        31L, 0, EnrollmentStatus.IN_PROGRESS
                )
        ));

        List<MyEnrolledCourseView> result = service.handle(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).courseId()).isEqualTo(20L);
    }

    @Test
    void 레슨이_없는_강의는_수강중_목록에_포함된다() {
        when(queryPort.findByMemberId(1L)).thenReturn(List.of(
                new MyEnrolledCourseQueryPort.MyEnrolledCourseData(
                        10L, "빈 강의", null,
                        0, 0,
                        null, null, 0, EnrollmentStatus.IN_PROGRESS
                )
        ));

        List<MyEnrolledCourseView> result = service.handle(1L);

        assertThat(result).hasSize(1);
    }

    private MyEnrolledCourseQueryPort.MyEnrolledCourseData data(Long courseId, LocalDateTime lastStudiedAt) {
        return new MyEnrolledCourseQueryPort.MyEnrolledCourseData(
                courseId,
                "Course " + courseId,
                null,
                0,
                1,
                lastStudiedAt,
                courseId * 10,
                0,
                EnrollmentStatus.IN_PROGRESS
        );
    }
}
