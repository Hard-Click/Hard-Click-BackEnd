package com.wanted.backend.domain.notice;

import com.wanted.backend.domain.notice.application.command.GetNoticeListCommand;
import com.wanted.backend.domain.notice.application.port.CourseInfoPort;
import com.wanted.backend.domain.notice.application.port.EnrolledCoursePort;
import com.wanted.backend.domain.notice.application.port.InstructorCoursePort;
import com.wanted.backend.domain.notice.application.port.NoticeReadPort;
import com.wanted.backend.domain.notice.application.result.NoticeDetailResult;
import com.wanted.backend.domain.notice.application.result.NoticeListResult;
import com.wanted.backend.domain.notice.application.service.NoticeQueryService;
import com.wanted.backend.domain.notice.domain.model.Notice;
import com.wanted.backend.domain.notice.domain.model.NoticeStatus;
import com.wanted.backend.domain.notice.domain.repository.NoticeRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticeQueryServiceTest {

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private CourseInfoPort courseInfoPort;

    @Mock
    private InstructorCoursePort instructorCoursePort;

    @Mock
    private EnrolledCoursePort enrolledCoursePort;

    @Mock
    private NoticeReadPort noticeReadPort;

    private NoticeQueryService noticeQueryService;

    private final Long courseId = 1L;
    private final Long noticeId = 5L;

    private void init() {
        noticeQueryService = new NoticeQueryService(
                noticeRepository, courseInfoPort, instructorCoursePort,
                enrolledCoursePort, noticeReadPort);
    }

    private Notice courseNotice() {
        return Notice.restore(
                noticeId, 10L, courseId, "3주차 과제 제출 안내", "이번 주 일요일까지 제출해주세요.",
                false, "COURSE", NoticeStatus.PUBLISHED,
                LocalDateTime.now(), LocalDateTime.now());
    }

    // ── getDetail: 강의 상세와 동일하게 공개 ─────────────────────────────

    @Test
    @DisplayName("비로그인 사용자도 강의 공지 상세를 조회할 수 있다")
    void getDetail_anonymousUserCanViewCourseNotice() {
        init();
        lenient().when(noticeRepository.findById(noticeId)).thenReturn(java.util.Optional.of(courseNotice()));
        lenient().when(courseInfoPort.getCourseNameByCourseId(courseId)).thenReturn("스프링 부트 강의");
        lenient().when(noticeRepository.findPreviousNotice(anyLong(), anyString(), anyLong()))
                .thenReturn(java.util.Optional.empty());

        NoticeDetailResult result = noticeQueryService.getDetail(noticeId, null, null);

        assertThat(result.noticeId()).isEqualTo(noticeId);
        assertThat(result.courseName()).isEqualTo("스프링 부트 강의");
    }

    @Test
    @DisplayName("수강하지 않은 로그인 사용자도 강의 공지 상세를 조회할 수 있다")
    void getDetail_nonEnrolledStudentCanViewCourseNotice() {
        init();
        Long otherMemberId = 999L;
        when(noticeRepository.findById(noticeId)).thenReturn(java.util.Optional.of(courseNotice()));
        when(courseInfoPort.getCourseNameByCourseId(courseId)).thenReturn("스프링 부트 강의");
        when(noticeRepository.findPreviousNotice(anyLong(), anyString(), anyLong()))
                .thenReturn(java.util.Optional.empty());

        NoticeDetailResult result = noticeQueryService.getDetail(noticeId, otherMemberId, "STUDENT");

        assertThat(result.noticeId()).isEqualTo(noticeId);
    }

    @Test
    @DisplayName("읽음 처리된 공지는 상세 조회 시 isRead=true로 내려온다")
    void getDetail_reflectsReadState() {
        init();
        Long memberId = 20L;
        when(noticeRepository.findById(noticeId)).thenReturn(java.util.Optional.of(courseNotice()));
        when(courseInfoPort.getCourseNameByCourseId(courseId)).thenReturn("스프링 부트 강의");
        when(noticeRepository.findPreviousNotice(anyLong(), anyString(), anyLong()))
                .thenReturn(java.util.Optional.empty());
        when(noticeReadPort.isRead(memberId, noticeId)).thenReturn(true);

        NoticeDetailResult result = noticeQueryService.getDetail(noticeId, memberId, "STUDENT");

        assertThat(result.isRead()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 공지사항 조회 시 실패한다")
    void getDetail_fail_noticeNotFound() {
        init();
        when(noticeRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> noticeQueryService.getDetail(999L, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.NOTICE_NOT_FOUND.getMessage());
    }

    // ── getList: courseId 지정 시 강의 상세와 동일하게 공개 ──────────────

    @Test
    @DisplayName("비로그인 사용자도 courseId로 강의 공지 목록을 조회할 수 있다")
    void getList_anonymousCanViewCourseNoticesByCourseId() {
        init();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notice> page = new PageImpl<>(List.of(courseNotice()), pageable, 1);

        when(noticeRepository.findCourseNotices(eq(courseId), anyString(), any(Pageable.class)))
                .thenReturn(page);
        when(courseInfoPort.getCourseNameByCourseId(courseId)).thenReturn("스프링 부트 강의");
        when(noticeReadPort.findReadNoticeIds(isNull(), anyList())).thenReturn(List.of());

        GetNoticeListCommand command = new GetNoticeListCommand(
                "COURSE", courseId, null, 0, 10, null, null);

        NoticeListResult result = noticeQueryService.getList(command);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).courseName()).isEqualTo("스프링 부트 강의");
    }

    @Test
    @DisplayName("비로그인 + courseId 없이 COURSE 공지 목록(내 수강 강의 모아보기)을 조회하면 실패한다")
    void getList_fail_anonymousWithoutCourseIdForAggregateView() {
        init();
        GetNoticeListCommand command = new GetNoticeListCommand(
                "COURSE", null, null, 0, 10, null, null);

        assertThatThrownBy(() -> noticeQueryService.getList(command))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.COURSE_ACCESS_DENIED.getMessage());
    }

    @Test
    @DisplayName("수강 중인 학생은 courseId 없이 자신의 수강 강의 공지 목록을 조회할 수 있다")
    void getList_enrolledStudentCanViewOwnCourseAggregate() {
        init();
        Long studentId = 20L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notice> page = new PageImpl<>(List.of(courseNotice()), pageable, 1);

        when(enrolledCoursePort.getEnrolledCourseIdsByMemberId(studentId)).thenReturn(List.of(courseId));
        when(noticeRepository.findCourseNoticesByIds(eq(List.of(courseId)), anyString(), any(Pageable.class)))
                .thenReturn(page);
        when(courseInfoPort.getCourseNamesByCourseIds(anyList())).thenReturn(java.util.Map.of(courseId, "스프링 부트 강의"));
        when(noticeReadPort.findReadNoticeIds(eq(studentId), anyList())).thenReturn(List.of());

        GetNoticeListCommand command = new GetNoticeListCommand(
                "COURSE", null, null, 0, 10, studentId, "STUDENT");

        NoticeListResult result = noticeQueryService.getList(command);

        assertThat(result.content()).hasSize(1);
    }

    @Test
    @DisplayName("GLOBAL 타입 공지는 비로그인 사용자도 조회할 수 있다")
    void getList_globalNoticesVisibleToAnonymous() {
        init();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notice> page = new PageImpl<>(List.of(), pageable, 0);

        when(noticeRepository.findGlobalNotices(anyString(), any(Pageable.class))).thenReturn(page);

        GetNoticeListCommand command = new GetNoticeListCommand(
                "GLOBAL", null, null, 0, 10, null, null);

        NoticeListResult result = noticeQueryService.getList(command);

        assertThat(result.content()).isEmpty();
    }
}
