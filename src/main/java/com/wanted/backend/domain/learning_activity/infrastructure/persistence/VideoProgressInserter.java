package com.wanted.backend.domain.learning_activity.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class VideoProgressInserter {

    private final SpringDataVideoProgressRepository repository;

    /**
     * 진도 행을 별도 트랜잭션에서 INSERT한다.
     *
     * <p>동시 요청이 같은 (member_id, video_id)를 INSERT하면 한쪽은 uk_video_progress_member_video에
     * 걸린다. 이때 바깥 트랜잭션에서 그대로 터뜨리면 트랜잭션이 rollback-only로 오염돼 경합에 밀린
     * 요청이 기존 행을 읽어 갱신하는 복구를 할 수 없다. REQUIRES_NEW로 분리해 실패를 이 트랜잭션
     * 안에 가둔다.
     *
     * <p>제약 위반을 호출부에서 잡을 수 있도록 saveAndFlush로 즉시 flush한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public VideoProgressJpaEntity insert(VideoProgressJpaEntity entity) {
        return repository.saveAndFlush(entity);
    }

    /**
     * 동시 INSERT 경합에서 밀린 요청의 복구 경로 — 먼저 커밋된 행을 읽어 갱신한다.
     *
     * <p><b>반드시 REQUIRES_NEW여야 한다.</b> 바깥 트랜잭션에서 읽으면 MySQL 기본 격리수준
     * (REPEATABLE READ)의 스냅샷이 경합 상대가 커밋하기 전(첫 조회 시점)에 고정돼 있어, 방금
     * 커밋된 행이 보이지 않는다({@link Optional#empty()}). 그러면 복구가 실패해 유니크 제약 위반이
     * 그대로 상승한다(C001). 새 트랜잭션=새 스냅샷에서 읽어 커밋된 행을 확실히 본다.
     *
     * <p>그래도 행이 없으면 진짜 제약 위반(NOT NULL·FK 등)이므로 {@link Optional#empty()}를 돌려
     * 호출부가 원래 예외를 던지게 한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<VideoProgressJpaEntity> updateExisting(
            Long memberId,
            Long videoId,
            Integer lastPositionSec,
            Integer watchTimeSec,
            Boolean completed,
            LocalDateTime completedAt,
            LocalDateTime updatedAt
    ) {
        return repository.findByMemberIdAndVideoId(memberId, videoId)
                .map(entity -> {
                    entity.updateProgress(lastPositionSec, watchTimeSec, completed, completedAt, updatedAt);
                    return repository.saveAndFlush(entity);
                });
    }
}
