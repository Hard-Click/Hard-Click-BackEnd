package com.wanted.backend.domain.notice;

import com.wanted.backend.domain.notice.application.command.CreateNoticeCommand;
import com.wanted.backend.domain.notice.application.policy.NoticeCreatePolicy;
import com.wanted.backend.domain.notice.application.port.AdminValidationPort;
import com.wanted.backend.domain.notice.application.port.NoticeReadPort;
import com.wanted.backend.domain.notice.application.service.NoticeCommandService;
import com.wanted.backend.domain.notice.domain.model.Notice;
import com.wanted.backend.domain.notice.domain.model.NoticeStatus;
import com.wanted.backend.domain.notice.domain.repository.NoticeRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoticeCommandServiceTest {

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private NoticeCreatePolicy noticeCreatePolicy;

    @Mock
    private NoticeReadPort noticeReadPort;

    @Mock
    private AdminValidationPort adminValidationPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private NoticeCommandService noticeCommandService;

    private final Long instructorId = 10L;
    private final Long courseId = 1L;

    @Test
    @DisplayName("강의 공지사항 작성 성공")
    void create_success() {
        // given
        CreateNoticeCommand command = new CreateNoticeCommand(
                instructorId, courseId,
                "3주차 과제 제출 안내",
                "이번 주 일요일까지 제출해주세요.",
                true
        );

        // Policy 검증 통과 (아무 예외 없음)
        doNothing().when(noticeCreatePolicy).validate(instructorId, courseId);

        // Repository save 시 noticeId=1 반환
        Notice savedNotice = Notice.restore(
                1L, instructorId, courseId,
                "3주차 과제 제출 안내",
                "이번 주 일요일까지 제출해주세요.",
                true, "COURSE", NoticeStatus.PUBLISHED,
                LocalDateTime.now(), LocalDateTime.now()
        );
        when(noticeRepository.save(any(Notice.class))).thenReturn(savedNotice);

        // when
        Long noticeId = noticeCommandService.create(command);

        // then
        assertThat(noticeId).isEqualTo(1L);

        // Policy 검증이 1번 호출됐는지 확인
        verify(noticeCreatePolicy, times(1)).validate(instructorId, courseId);

        // Repository save가 1번 호출됐는지 확인
        verify(noticeRepository, times(1)).save(any(Notice.class));
    }

    @Test
    @DisplayName("담당 강사가 아닌 경우 공지 작성 실패")
    void create_fail_notInstructor() {
        // given
        Long otherMemberId = 99L;
        CreateNoticeCommand command = new CreateNoticeCommand(
                otherMemberId, courseId,
                "공지사항",
                "내용",
                false
        );

        // Policy에서 예외 발생
        doThrow(new BusinessException(ErrorCode.NOTICE_NOT_AUTHORIZED))
                .when(noticeCreatePolicy).validate(otherMemberId, courseId);

        // when & then
        assertThatThrownBy(() -> noticeCommandService.create(command))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.NOTICE_NOT_AUTHORIZED.getMessage());

        // Repository save는 호출되지 않아야 함
        verify(noticeRepository, never()).save(any(Notice.class));
    }

    @Test
    @DisplayName("존재하지 않는 강의 ID로 공지 작성 실패")
    void create_fail_courseNotFound() {
        // given
        CreateNoticeCommand command = new CreateNoticeCommand(
                instructorId, 999L,
                "공지사항",
                "내용",
                false
        );

        // Policy에서 예외 발생
        doThrow(new BusinessException(ErrorCode.COURSE_NOT_FOUND))
                .when(noticeCreatePolicy).validate(instructorId, 999L);

        // when & then
        assertThatThrownBy(() -> noticeCommandService.create(command))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.COURSE_NOT_FOUND.getMessage());

        // Repository save는 호출되지 않아야 함
        verify(noticeRepository, never()).save(any(Notice.class));
    }

    @Test
    @DisplayName("공지 읽음 처리: 공지가 존재하면 회원 기준으로 읽음 저장을 위임한다")
    void markAsRead_delegatesWhenNoticeExists() {
        Long memberId = 20L;
        Long noticeId = 4L;
        Notice notice = Notice.restore(noticeId, instructorId, courseId, "제목", "내용", false,
                "COURSE", NoticeStatus.PUBLISHED, LocalDateTime.now(), LocalDateTime.now());
        when(noticeRepository.findById(noticeId)).thenReturn(java.util.Optional.of(notice));

        noticeCommandService.markAsRead(memberId, noticeId);

        verify(noticeReadPort, times(1)).markRead(memberId, noticeId);
    }

    @Test
    @DisplayName("공지 읽음 처리: 존재하지 않는 공지면 NOTICE_NOT_FOUND, 읽음 저장은 하지 않는다")
    void markAsRead_throwsWhenNoticeNotFound() {
        Long memberId = 20L;
        Long noticeId = 999L;
        when(noticeRepository.findById(noticeId)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> noticeCommandService.markAsRead(memberId, noticeId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.NOTICE_NOT_FOUND.getMessage());

        verify(noticeReadPort, never()).markRead(any(), any());
    }
}