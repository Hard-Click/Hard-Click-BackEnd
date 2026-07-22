package com.wanted.backend.domain.learning_activity.application.service;

import com.wanted.backend.domain.learning_activity.application.command.MemberVideoCommand;
import com.wanted.backend.domain.learning_activity.application.policy.VideoCompletionPolicy;
import com.wanted.backend.domain.learning_activity.application.usecase.CompleteVideoUseCase;
import com.wanted.backend.domain.learning_activity.domain.event.VideoCompletedEvent;
import com.wanted.backend.domain.learning_activity.domain.model.VideoAccessInfo;
import com.wanted.backend.domain.learning_activity.domain.model.VideoProgress;
import com.wanted.backend.domain.learning_activity.domain.repository.VideoProgressRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CompleteVideoService implements CompleteVideoUseCase {

    private static final LearningActivityAction ACTION = LearningActivityAction.COMPLETE_VIDEO;

    private final PlayableVideoProgressReader playableVideoProgressReader;
    private final VideoProgressRepository videoProgressRepository;
    private final VideoCompletionPolicy videoCompletionPolicy;
    private final LearningActivityMetricRecorder metricRecorder;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void handle(MemberVideoCommand command) {
        String errorCode = "UNKNOWN";
        try {
            Long memberId = command.memberId();
            Long videoId = command.videoId();

            PlayableVideoProgressReader.PlayableVideoProgress playable =
                    playableVideoProgressReader.get(memberId, videoId);
            VideoAccessInfo accessInfo = playable.accessInfo();
            VideoProgress progress = playable.progress();

            // 이미 완료된 영상이면 완료는 멱등하다 — 재저장·이벤트 재발행 없이 성공으로 끝낸다.
            // VideoCompletedEvent 를 '미완료→완료' 전이에서 1회만 발행해야, 반복 complete 호출(더블클릭·FE 재시도)에
            // 랭킹·수강량 잔디가 이벤트당 +1 로 중복 집계되지 않는다.
            // (동시 최초 완료의 경합까지 막으려면 소비자 멱등키/outbox 가 필요 — 별도 후속 이슈.)
            if (progress.isCompleted()) {
                errorCode = null;
                return;
            }

            if (!videoCompletionPolicy.canComplete(effectiveProgressSeconds(progress), accessInfo.durationSeconds())) {
                throw new BusinessException(ErrorCode.VIDEO_COMPLETION_CONDITION_NOT_MET);
            }

            videoProgressRepository.save(progress.complete(LocalDateTime.now()));
            eventPublisher.publishEvent(VideoCompletedEvent.of(memberId, videoId, accessInfo.courseId()));
            errorCode = null;
        } catch (BusinessException e) {
            errorCode = e.getErrorCode().name();
            throw e;
        } finally {
            try {
                metricRecorder.recordResult(ACTION, errorCode);
            } catch (RuntimeException e) {
                // metric failure must not affect the business transaction
                log.warn("learning activity metric record failed: action={}, errorCode={}", ACTION, errorCode, e);
            }
        }
    }

    private int effectiveProgressSeconds(VideoProgress progress) {
        int watchTimeSec = progress.watchTimeSec() == null ? 0 : progress.watchTimeSec();
        int lastPositionSec = progress.lastPositionSec() == null ? 0 : progress.lastPositionSec();

        return Math.max(watchTimeSec, lastPositionSec);
    }
}
