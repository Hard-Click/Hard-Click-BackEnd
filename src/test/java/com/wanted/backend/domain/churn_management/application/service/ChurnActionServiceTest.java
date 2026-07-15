package com.wanted.backend.domain.churn_management.application.service;

import com.wanted.backend.domain.churn_management.application.port.ChurnQueryPort;
import com.wanted.backend.domain.notification.application.usecase.NotificationCommandUseCase;
import com.wanted.backend.domain.notification.domain.model.NotificationType;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChurnActionServiceTest {

    @InjectMocks
    private ChurnActionService churnActionService;

    @Mock
    private ChurnQueryPort churnQueryPort;

    @Mock
    private NotificationCommandUseCase notificationCommandUseCase;

    @Test
    @DisplayName("독려 알림은 enrollment 소유 회원에게 스케줄 화면 링크로 발송된다")
    void nudge_sendsNoticeToResolvedMember() {
        // given
        given(churnQueryPort.findMemberIdByEnrollmentId(10L)).willReturn(500L);

        // when
        churnActionService.nudge(10L);

        // then
        verify(notificationCommandUseCase).send(
                eq(500L),
                eq(NotificationType.NOTICE),
                anyString(),
                eq("/mypage/schedule"));
    }

    @Test
    @DisplayName("스케줄 재조정 권유 알림은 reflow 링크로 발송된다")
    void suggestReflow_sendsNoticeWithReflowLink() {
        // given
        given(churnQueryPort.findMemberIdByEnrollmentId(10L)).willReturn(500L);

        // when
        churnActionService.suggestReflow(10L);

        // then
        verify(notificationCommandUseCase).send(
                eq(500L),
                eq(NotificationType.NOTICE),
                anyString(),
                eq("/mypage/schedule?reflow=1"));
    }

    @Test
    @DisplayName("이탈 위험 기록이 없는 enrollment면 독려 알림에서 예외가 발생하고 발송하지 않는다")
    void nudge_fail_whenRiskRecordNotFound() {
        // given
        given(churnQueryPort.findMemberIdByEnrollmentId(999L)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> churnActionService.nudge(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.CHURN_RISK_NOT_FOUND.getMessage());

        verify(notificationCommandUseCase, never()).send(anyLong(), any(), anyString(), any());
    }

    @Test
    @DisplayName("이탈 위험 기록이 없는 enrollment면 재조정 권유에서도 예외가 발생한다")
    void suggestReflow_fail_whenRiskRecordNotFound() {
        // given
        given(churnQueryPort.findMemberIdByEnrollmentId(999L)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> churnActionService.suggestReflow(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.CHURN_RISK_NOT_FOUND.getMessage());

        verify(notificationCommandUseCase, never()).send(anyLong(), any(), anyString(), any());
    }
}
