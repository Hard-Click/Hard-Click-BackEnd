package com.wanted.backend.domain.churn_management.application.service;

import com.wanted.backend.domain.churn_management.application.port.ChurnQueryPort;
import com.wanted.backend.domain.churn_management.application.usecase.ChurnActionUseCase;
import com.wanted.backend.domain.notification.application.usecase.NotificationCommandUseCase;
import com.wanted.backend.domain.notification.domain.model.NotificationType;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChurnActionService implements ChurnActionUseCase {

    // 학생이 보는 스케줄 화면 경로(알림 클릭 시 이동). FE 라우팅 확정되면 조정.
    private static final String SCHEDULE_REDIRECT_URL = "/mypage/schedule";
    private static final String REFLOW_REDIRECT_URL = "/mypage/schedule?reflow=1";

    private static final String NUDGE_MESSAGE = "요즘 학습이 뜸해요. 오늘 짧게라도 다시 시작해볼까요?";
    private static final String REFLOW_MESSAGE = "일정이 밀렸어요. 스케줄을 재조정하면 부담을 덜 수 있어요. 지금 확인해보세요.";

    private final ChurnQueryPort churnQueryPort;
    // TODO: 전용 NotificationType(CHURN_NUDGE 등)이 없어 NOTICE 재사용 중 - 필요 시 enum 추가.
    private final NotificationCommandUseCase notificationCommandUseCase;

    @Override
    public void nudge(Long enrollmentId) {
        Long memberId = resolveMemberId(enrollmentId);
        notificationCommandUseCase.send(memberId, NotificationType.NOTICE, NUDGE_MESSAGE, SCHEDULE_REDIRECT_URL);
    }

    @Override
    public void suggestReflow(Long enrollmentId) {
        Long memberId = resolveMemberId(enrollmentId);
        notificationCommandUseCase.send(memberId, NotificationType.NOTICE, REFLOW_MESSAGE, REFLOW_REDIRECT_URL);
    }

    private Long resolveMemberId(Long enrollmentId) {
        Long memberId = churnQueryPort.findMemberIdByEnrollmentId(enrollmentId);
        if (memberId == null) {
            throw new BusinessException(ErrorCode.CHURN_RISK_NOT_FOUND);
        }
        return memberId;
    }
}
