package com.wanted.backend.domain.notice;

import com.wanted.backend.domain.notice.application.policy.NoticeCreatePolicy;
import com.wanted.backend.domain.notice.application.port.AdminValidationPort;
import com.wanted.backend.domain.notice.application.port.CourseInstructorPort;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoticeCreatePolicyTest {

    @Mock
    private CourseInstructorPort courseInstructorPort;

    @Mock
    private AdminValidationPort adminValidationPort;

    @InjectMocks
    private NoticeCreatePolicy noticeCreatePolicy;

    private final Long courseId = 1L;
    private final Long instructorId = 10L;
    private final Long otherMemberId = 99L;

    @BeforeEach
    void setUp() {
        // courseId=1 강의의 담당 강사는 instructorId=10
        lenient().when(courseInstructorPort.getInstructorIdByCourseId(courseId))
                .thenReturn(instructorId);
    }

    @Test
    @DisplayName("담당 강사가 공지 작성 시 검증 통과")
    void validate_success() {
        // given & when & then
        assertThatNoException()
                .isThrownBy(() -> noticeCreatePolicy.validate(instructorId, courseId));
    }

    @Test
    @DisplayName("담당 강사가 아닌 경우 NOTICE_NOT_AUTHORIZED 예외 발생")
    void validate_fail_notInstructor() {
        // given & when & then
        assertThatThrownBy(() -> noticeCreatePolicy.validate(otherMemberId, courseId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.NOTICE_NOT_AUTHORIZED.getMessage());
    }

    @Test
    @DisplayName("존재하지 않는 강의 ID로 조회 시 COURSE_NOT_FOUND 예외 발생")
    void validate_fail_courseNotFound() {
        // given
        Long nonExistCourseId = 999L;
        when(courseInstructorPort.getInstructorIdByCourseId(nonExistCourseId))
                .thenThrow(new BusinessException(ErrorCode.COURSE_NOT_FOUND2));

        // when & then
        assertThatThrownBy(() -> noticeCreatePolicy.validate(instructorId, nonExistCourseId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.COURSE_NOT_FOUND2.getMessage());
    }
}