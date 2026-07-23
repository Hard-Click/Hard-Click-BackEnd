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

public interface SpringDataVideoCompletionOutboxRepository
        extends JpaRepository<VideoCompletionOutboxJpaEntity, Long> {

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
