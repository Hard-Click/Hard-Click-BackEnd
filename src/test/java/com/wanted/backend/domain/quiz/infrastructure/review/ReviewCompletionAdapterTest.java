package com.wanted.backend.domain.quiz.infrastructure.review;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewCompletionAdapterTest {

    @Test
    @DisplayName("복습 간격은 reps가 커질수록 늘고(3→7→14→30), 배열 상한을 넘으면 마지막 값으로 clamp된다")
    void nextIntervalDaysGrowsWithRepsAndClamps() {
        assertThat(ReviewCompletionAdapter.nextIntervalDays(0)).isEqualTo(3);
        assertThat(ReviewCompletionAdapter.nextIntervalDays(1)).isEqualTo(7);
        assertThat(ReviewCompletionAdapter.nextIntervalDays(2)).isEqualTo(14);
        assertThat(ReviewCompletionAdapter.nextIntervalDays(3)).isEqualTo(30);
        // 상한 초과 → 마지막 값 유지
        assertThat(ReviewCompletionAdapter.nextIntervalDays(4)).isEqualTo(30);
        assertThat(ReviewCompletionAdapter.nextIntervalDays(100)).isEqualTo(30);
    }

    @Test
    @DisplayName("비정상 음수 reps도 하한(첫 간격)으로 안전하게 clamp된다")
    void nextIntervalDaysClampsNegativeReps() {
        assertThat(ReviewCompletionAdapter.nextIntervalDays(-1)).isEqualTo(3);
    }
}
