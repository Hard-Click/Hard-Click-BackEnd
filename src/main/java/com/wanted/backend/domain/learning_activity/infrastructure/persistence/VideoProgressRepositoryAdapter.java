package com.wanted.backend.domain.learning_activity.infrastructure.persistence;

import com.wanted.backend.domain.learning_activity.domain.model.VideoProgress;
import com.wanted.backend.domain.learning_activity.domain.repository.VideoProgressRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VideoProgressRepositoryAdapter implements VideoProgressRepository {

    private final SpringDataVideoProgressRepository repository;
    private final VideoProgressInserter inserter;

    @Override
    public Optional<VideoProgress> findByMemberIdAndVideoId(Long memberId, Long videoId) {
        return repository.findByMemberIdAndVideoId(memberId, videoId)
                .map(this::toDomain);
    }

    @Override
    @Transactional
    public VideoProgress save(VideoProgress progress) {
        LocalDateTime now = LocalDateTime.now();

        if (progress.id() == null) {
            return insert(progress, now);
        }

        VideoProgressJpaEntity entity = repository.findById(progress.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND));

        entity.updateProgress(
                progress.lastPositionSec(),
                progress.watchTimeSec(),
                progress.completed(),
                progress.completedAt(),
                now
        );

        return toDomain(repository.save(entity));
    }

    /**
     * 아직 행이 없는 상태에서 동시 요청이 들어오면 모두 "없음"으로 읽고 각자 INSERT를 시도한다.
     * 유니크 제약이 한쪽을 막아주므로, 밀린 쪽은 먼저 만들어진 행을 읽어 갱신한다.
     */
    private VideoProgress insert(VideoProgress progress, LocalDateTime now) {
        try {
            return toDomain(inserter.insert(new VideoProgressJpaEntity(
                    progress.memberId(),
                    progress.courseId(),
                    progress.videoId(),
                    progress.lastPositionSec(),
                    progress.watchTimeSec(),
                    progress.completed(),
                    progress.completedAt(),
                    now
            )));
        } catch (DataIntegrityViolationException violation) {
            // NOT NULL·FK 위반도 같은 타입이라, 무조건 경합으로 단정하면 진짜 원인이 가려진다.
            // 실제로 행이 생겨 있을 때만 경합으로 보고 복구하고, 아니면 원래 예외를 그대로 올린다.
            VideoProgressJpaEntity entity = repository
                    .findByMemberIdAndVideoId(progress.memberId(), progress.videoId())
                    .orElseThrow(() -> violation);

            entity.updateProgress(
                    progress.lastPositionSec(),
                    progress.watchTimeSec(),
                    progress.completed(),
                    progress.completedAt(),
                    now
            );

            return toDomain(repository.save(entity));
        }
    }

    private VideoProgress toDomain(VideoProgressJpaEntity entity) {
        return new VideoProgress(
                entity.getId(),
                entity.getMemberId(),
                entity.getCourseId(),
                entity.getVideoId(),
                entity.getLastPositionSec(),
                entity.getWatchTimeSec(),
                entity.getCompleted(),
                entity.getCompletedAt()
        );
    }
}
