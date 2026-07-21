package com.wanted.backend.domain.community.application.service;

import com.wanted.backend.domain.community.application.command.CreateReportCommand;
import com.wanted.backend.domain.community.application.policy.CommunityAccessPolicy;
import com.wanted.backend.domain.community.application.port.MemberAutoSuspendPort;
import com.wanted.backend.domain.community.domain.event.ReportCreatedEvent;
import com.wanted.backend.domain.community.domain.model.Post;
import com.wanted.backend.domain.community.domain.model.ReportType;
import com.wanted.backend.domain.community.domain.model.TargetType;
import com.wanted.backend.domain.community.domain.repository.CommentRepository;
import com.wanted.backend.domain.community.domain.repository.PostRepository;
import com.wanted.backend.domain.community.domain.repository.ReportRepository;
import com.wanted.backend.domain.community.domain.repository.ReviewRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 신고 알림 발송 시점 회귀 방지.
 * 관리자 신고 목록은 3건 이상 누적 대상만 노출하므로, 알림도 3건째부터 발송돼야 한다.
 * (1·2건째는 접수만, 3건째부터 ReportCreatedEvent 발행)
 */
class ReportCommandServiceTest {

    private static final Long REPORTER_ID = 1L;
    private static final Long TARGET_ID = 100L;
    private static final Long REPORTED_MEMBER_ID = 500L;

    private ReportRepository reportRepository;
    private CommunityAccessPolicy communityAccessPolicy;
    private ApplicationEventPublisher eventPublisher;
    private ReportCommandService service;

    @BeforeEach
    void setUp() {
        reportRepository = mock(ReportRepository.class);
        PostRepository postRepository = mock(PostRepository.class);
        CommentRepository commentRepository = mock(CommentRepository.class);
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        communityAccessPolicy = mock(CommunityAccessPolicy.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        MemberAutoSuspendPort memberAutoSuspendPort = mock(MemberAutoSuspendPort.class);

        Post post = mock(Post.class);
        when(post.getAuthorId()).thenReturn(REPORTED_MEMBER_ID);
        when(postRepository.findById(TARGET_ID)).thenReturn(Optional.of(post));
        when(reportRepository.existsByReporterIdAndTargetTypeAndTargetId(anyLong(), any(), anyLong()))
                .thenReturn(false);
        when(reportRepository.save(any())).thenReturn(9L);
        when(reportRepository.countByReportedMemberId(REPORTED_MEMBER_ID)).thenReturn(1); // 자동정지(50) 미만

        service = new ReportCommandService(
                reportRepository, postRepository, commentRepository, reviewRepository,
                communityAccessPolicy, eventPublisher, memberAutoSuspendPort);
    }

    private void reportWithCumulativeCount(int count) {
        when(reportRepository.countByTargetTypeAndTargetId(TargetType.POST, TARGET_ID)).thenReturn(count);
        service.create(new CreateReportCommand(
                REPORTER_ID, TargetType.POST, TARGET_ID, List.of(ReportType.SPAM), "사유"));
    }

    @Test
    @DisplayName("1·2건째 신고는 접수만 되고 알림 이벤트를 발행하지 않는다")
    void doesNotPublishBelowThreshold() {
        reportWithCumulativeCount(1);
        reportWithCumulativeCount(2);

        verify(eventPublisher, never()).publishEvent(any(ReportCreatedEvent.class));
    }

    @Test
    @DisplayName("3건째 누적 시 알림 이벤트를 1회 발행한다")
    void publishesAtThreshold() {
        reportWithCumulativeCount(3);

        verify(eventPublisher, times(1)).publishEvent(any(ReportCreatedEvent.class));
    }

    @Test
    @DisplayName("3건 초과(4·5건째 등)에도 매번 알림 이벤트를 발행한다")
    void publishesAboveThreshold() {
        reportWithCumulativeCount(5);

        verify(eventPublisher, times(1)).publishEvent(any(ReportCreatedEvent.class));
    }

    @Test
    @DisplayName("같은 대상에 이미 신고한 사용자가 재신고하면 REPORT_ALREADY_EXISTS로 막고 저장·알림하지 않는다")
    void rejectsDuplicateReport() {
        when(reportRepository.existsByReporterIdAndTargetTypeAndTargetId(anyLong(), any(), anyLong()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateReportCommand(
                REPORTER_ID, TargetType.POST, TARGET_ID, List.of(ReportType.SPAM), "사유")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.REPORT_ALREADY_EXISTS);

        verify(reportRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("접근 정책 검증 실패는 그대로 전파되고 저장·알림하지 않는다")
    void propagatesAccessPolicyFailure() {
        doThrow(new BusinessException(ErrorCode.COMMUNITY_ACCESS_DENIED))
                .when(communityAccessPolicy).validateAccess(REPORTER_ID);

        assertThatThrownBy(() -> service.create(new CreateReportCommand(
                REPORTER_ID, TargetType.POST, TARGET_ID, List.of(ReportType.SPAM), "사유")))
                .isInstanceOf(BusinessException.class);

        verify(reportRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
