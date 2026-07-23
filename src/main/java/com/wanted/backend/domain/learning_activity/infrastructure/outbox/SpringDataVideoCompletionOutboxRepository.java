package com.wanted.backend.domain.learning_activity.infrastructure.outbox;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SpringDataVideoCompletionOutboxRepository
        extends JpaRepository<VideoCompletionOutboxJpaEntity, Long> {

    /**
     * 종료 전이(markDone/markFailed)용 행을 비관적 잠금으로 조회한다.
     *
     * <p>스냅샷 격리(REPEATABLE_READ)에서 일반 조회는 트랜잭션 시작 시점의 attempts를 보므로, 그 사이 다른
     * relay가 재선점(attempts 증가)한 것을 놓쳐 소유권 검증이 뚫릴 수 있다. 잠금 조회는 최신 커밋값을 읽어
     * ({@code FOR UPDATE}) lease 세대 비교를 안전하게 만든다. (claimBatch와 달리 SKIP_LOCKED 힌트는 주지
     * 않아, 잠시 대기하더라도 정확한 종료 전이를 보장한다.)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<VideoCompletionOutboxJpaEntity> findWithLockById(Long id);

    /**
     * 처리 가능한(due) 행을 잠그며 가져온다 — 여러 인스턴스가 동시에 폴링해도 겹치지 않도록 행 잠금 + SKIP LOCKED.
     *
     * <p>PESSIMISTIC_WRITE는 {@code FOR UPDATE}를, lock.timeout 힌트 -2(Hibernate SKIP_LOCKED)는 잠긴 행
     * 건너뛰기를 붙인다. LIMIT은 {@link Pageable}로 준다. PENDING(신규·재시도 예정)뿐 아니라 relay가 중간에
     * 죽어 PROCESSING으로 남고 가시성 타임아웃(next_attempt_at)이 지난 행도 다시 집어 재처리한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    List<VideoCompletionOutboxJpaEntity> findByStatusInAndNextAttemptAtLessThanEqualOrderByNextAttemptAt(
            Collection<OutboxStatus> statuses,
            LocalDateTime now,
            Pageable pageable
    );
}
