package com.wanted.backend.domain.learning_activity.application.service;

import com.wanted.backend.domain.learning_activity.application.command.MemberVideoCommand;
import com.wanted.backend.domain.learning_activity.application.outbox.VideoCompletionOutboxStore;
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
    private final VideoCompletionOutboxStore videoCompletionOutboxStore;

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
            VideoCompletedEvent event = VideoCompletedEvent.of(memberId, videoId, accessInfo.courseId());
            // 즉시경로: 상태기반·멱등인 EnrollmentStatusUpdater(AFTER_COMMIT)만 이 이벤트를 구독한다.
            eventPublisher.publishEvent(event);
            // durable 경로: 랭킹·수강량 잔디는 완료 트랜잭션과 '같은 커밋'으로 outbox에 적재해, 크래시·다운스트림
            // 장애로도 유실되지 않게 한다. 실제 전달·재시도는 relay가 맡는다(소비자는 멱등).
            videoCompletionOutboxStore.enqueue(
                    event.memberId(), event.videoId(), event.courseId(), event.occurredAt());
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
