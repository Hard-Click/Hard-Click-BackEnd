package com.wanted.backend.domain.learning_activity.infrastructure.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 종료 전이(markDone/markFailed)의 lease 세대 소유권 검증 — 가시성 타임아웃 후 다른 relay가 재선점한 행을
 * 뒤늦게 깨어난 이전 relay가 덮어쓰지 못함을 확인한다.
 */
class VideoCompletionOutboxStoreAdapterTest {

    private SpringDataVideoCompletionOutboxRepository repository;
    private VideoCompletionOutboxStoreAdapter adapter;

    private static final LocalDateTime DEADLINE = LocalDateTime.of(2026, 1, 3, 0, 2);

    @BeforeEach
    void setUp() {
        repository = mock(SpringDataVideoCompletionOutboxRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-01-03T00:00:00Z"), ZoneOffset.UTC);
        adapter = new VideoCompletionOutboxStoreAdapter(repository, clock);
    }

    private VideoCompletionOutboxJpaEntity claimedRow(int generations) {
        VideoCompletionOutboxJpaEntity row = new VideoCompletionOutboxJpaEntity(
                77L, 55L, 42L,
                LocalDateTime.of(2026, 1, 3, 0, 0),
                LocalDateTime.of(2026, 1, 3, 0, 0));
        for (int i = 0; i < generations; i++) {
            row.markProcessing(DEADLINE);   // 매 선점마다 attempts(=lease 세대) 증가
        }
        return row;
    }

    @Test
    void 소유_세대의_markDone은_DONE으로_전이한다() {
        VideoCompletionOutboxJpaEntity row = claimedRow(1);   // attempts=1, PROCESSING
        when(repository.findWithLockById(eq(1L))).thenReturn(Optional.of(row));

        adapter.markDone(1L, 1);

        assertThat(row.getStatus()).isEqualTo(OutboxStatus.DONE);
    }

    @Test
    void 재선점된_행은_이전_세대의_markDone을_무시한다() {
        VideoCompletionOutboxJpaEntity row = claimedRow(2);   // 다른 relay가 재선점 → attempts=2, PROCESSING
        when(repository.findWithLockById(eq(1L))).thenReturn(Optional.of(row));

        adapter.markDone(1L, 1);   // 이전 세대(1)로 종료 시도

        assertThat(row.getStatus()).isEqualTo(OutboxStatus.PROCESSING);   // DONE으로 덮어써지지 않는다
    }

    @Test
    void 재선점된_행은_이전_세대의_markFailed로_상태가_되돌려지지_않는다() {
        VideoCompletionOutboxJpaEntity row = claimedRow(2);   // 재선점 → attempts=2, PROCESSING
        when(repository.findWithLockById(eq(1L))).thenReturn(Optional.of(row));

        adapter.markFailed(1L, "boom", 1);   // 이전 세대(1)로 실패 처리 시도

        assertThat(row.getStatus()).isEqualTo(OutboxStatus.PROCESSING);   // PENDING/DEAD로 되돌아가지 않는다
    }
}
