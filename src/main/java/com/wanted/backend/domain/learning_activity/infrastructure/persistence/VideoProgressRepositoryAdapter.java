package com.wanted.backend.domain.learning_activity.infrastructure.persistence;

import com.wanted.backend.domain.learning_activity.domain.model.VideoProgress;
import com.wanted.backend.domain.learning_activity.domain.repository.VideoProgressRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VideoProgressRepositoryAdapter implements VideoProgressRepository {

    // 첫 시청 동시 INSERT 경합을 식별하는 유니크 제약. 이 위반일 때만 기존 행을 읽어 복구한다.
    private static final String MEMBER_VIDEO_UNIQUE_CONSTRAINT = "uk_video_progress_member_video";

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
            // 복구는 (member_id, video_id) 유니크 경합일 때로 한정한다. NOT NULL·FK·CHECK 등 다른
            // 제약 위반은 진짜 오류이므로, 행 존재만 보고 복구하면 오류를 숨기고 기존 행까지 덮어쓴다.
            // 따라서 위반의 제약 이름이 uk_video_progress_member_video일 때만 복구하고, 아니면 전파한다.
            if (!isMemberVideoUniqueViolation(violation)) {
                throw violation;
            }

            // 복구 read는 반드시 새 트랜잭션(inserter.updateExisting=REQUIRES_NEW)에서 해야 한다.
            // 이 바깥 트랜잭션의 스냅샷은 경합 상대가 커밋하기 전(reader의 첫 조회 시점)에 고정돼
            // 있어(MySQL REPEATABLE READ), 방금 커밋된 행을 여기서 읽으면 보이지 않아 복구가 실패한다.
            return inserter.updateExisting(
                            progress.memberId(),
                            progress.videoId(),
                            progress.lastPositionSec(),
                            progress.watchTimeSec(),
                            progress.completed(),
                            progress.completedAt(),
                            now
                    )
                    .map(this::toDomain)
                    .orElseThrow(() -> violation);
        }
    }

    // 예외 원인 체인을 훑어 (member_id, video_id) 유니크 제약 위반인지 판정한다.
    // getConstraintName()이 방언/드라이버에 따라 null일 수 있어(H2에서 확인됨) 메시지 폴백을 함께 본다.
    // 제약 이름엔 스키마·인덱스 접두/접미사가 붙을 수 있어 부분 일치(대소문자 무시)로 본다.
    // NOT NULL·FK 등 다른 위반의 메시지엔 이 제약 이름이 없으므로 오탐하지 않는다.
    private boolean isMemberVideoUniqueViolation(Throwable violation) {
        for (Throwable cause = violation; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException constraintViolation
                    && containsConstraint(constraintViolation.getConstraintName())) {
                return true;
            }
            if (containsConstraint(cause.getMessage())) {
                return true;
            }
        }
        return false;
    }

    private boolean containsConstraint(String text) {
        return text != null && text.toLowerCase().contains(MEMBER_VIDEO_UNIQUE_CONSTRAINT);
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
