package com.wanted.backend.reviewloop.judge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 드라이버 예산 순수 로직 — 증가·한도·브랜치 리셋 (파일 IO는 main이 담당). */
class DriverBudgetTest {

    private DriverBudget.State fresh(String branch) {
        return DriverBudget.applied(null, branch, "--status");   // 저장 없음 → 리셋
    }

    @Test
    @DisplayName("저장이 없으면 현재 브랜치로 0에서 시작")
    void startsFreshWhenNoState() {
        DriverBudget.State s = fresh("main");
        assertThat(s.branch()).isEqualTo("main");
        assertThat(s.autofix()).isZero();
        assertThat(s.total()).isZero();
        assertThat(s.exhausted()).isFalse();
    }

    @Test
    @DisplayName("AutoFix는 3라운드까지 허용, 4라운드째 초과")
    void autofixAllowsThreeRoundsThenExhausts() {
        DriverBudget.State s = fresh("b");
        for (int i = 0; i < 3; i++) {
            s = DriverBudget.applied(s, "b", "--inc-autofix");
            assertThat(s.exhausted()).as("라운드 %d", i + 1).isFalse();
        }
        assertThat(s.autofix()).isEqualTo(3);

        s = DriverBudget.applied(s, "b", "--inc-autofix");   // 4라운드째
        assertThat(s.autofix()).isEqualTo(4);
        assertThat(s.exhausted()).isTrue();
    }

    @Test
    @DisplayName("Total은 6회까지 허용, 7회째 초과")
    void totalAllowsSixThenExhausts() {
        DriverBudget.State s = fresh("b");
        for (int i = 0; i < 6; i++) {
            s = DriverBudget.applied(s, "b", "--inc-total");
        }
        assertThat(s.exhausted()).isFalse();

        s = DriverBudget.applied(s, "b", "--inc-total");   // 7회째
        assertThat(s.exhausted()).isTrue();
    }

    @Test
    @DisplayName("브랜치가 바뀌면 리셋된다 (새 작업=새 예산)")
    void resetsOnBranchChange() {
        DriverBudget.State onA = DriverBudget.applied(fresh("A"), "A", "--inc-autofix");
        assertThat(onA.autofix()).isEqualTo(1);

        // 같은 저장 상태(브랜치 A)를 브랜치 B로 조회 → 리셋
        DriverBudget.State onB = DriverBudget.applied(onA, "B", "--status");
        assertThat(onB.branch()).isEqualTo("B");
        assertThat(onB.autofix()).isZero();
    }

    @Test
    @DisplayName("--reset은 카운트를 0으로")
    void resetZeroesCounts() {
        DriverBudget.State s = DriverBudget.applied(fresh("b"), "b", "--inc-autofix");
        s = DriverBudget.applied(s, "b", "--inc-total");
        s = DriverBudget.applied(s, "b", "--reset");

        assertThat(s.autofix()).isZero();
        assertThat(s.total()).isZero();
    }
}
