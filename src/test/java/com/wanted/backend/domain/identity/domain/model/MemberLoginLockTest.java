package com.wanted.backend.domain.identity.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gate 1 · CRITICAL 규칙 SEC_002 — 로그인 5회 연속 실패 시 계정 잠금(명세 고정값)을 코드로 못박는다.
 * Member 도메인 로직이라 Spring·mock 없이 결정론 검증.
 */
class MemberLoginLockTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 14, 10, 0);

    private Member newMember() {
        return Member.create("user", "a@gmail.com", "pw", "이름", "M",
                LocalDate.of(2000, 1, 1), "01012345678", null, Role.STUDENT, true);
    }

    @Test
    @DisplayName("SEC_002: 4회 실패까지는 잠기지 않는다")
    void notLockedBeforeFifthFailure() {
        Member member = newMember();
        for (int i = 0; i < 4; i++) {
            member.loginFailed(NOW);
        }
        assertThat(member.getLoginFailCount()).isEqualTo(4);
        assertThat(member.isLocked()).isFalse();
    }

    @Test
    @DisplayName("SEC_002: 5회째 실패에 계정이 잠긴다")
    void lockedOnFifthFailure() {
        Member member = newMember();
        for (int i = 0; i < 5; i++) {
            member.loginFailed(NOW);
        }
        assertThat(member.getLoginFailCount()).isEqualTo(5);
        assertThat(member.isLocked()).isTrue();
        assertThat(member.getLockedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("SEC_002: 잠긴 뒤 추가 실패는 카운트를 늘리지 않는다(5에서 멈춤)")
    void furtherFailuresIgnoredWhenLocked() {
        Member member = newMember();
        for (int i = 0; i < 7; i++) {
            member.loginFailed(NOW);
        }
        assertThat(member.getLoginFailCount()).isEqualTo(5);
        assertThat(member.isLocked()).isTrue();
    }

    @Test
    @DisplayName("SEC_002: 로그인 성공 시 실패 카운트·잠금이 초기화된다")
    void successResetsFailCountAndLock() {
        Member member = newMember();
        for (int i = 0; i < 3; i++) {
            member.loginFailed(NOW);
        }
        member.loginSuccess(NOW);
        assertThat(member.getLoginFailCount()).isZero();
        assertThat(member.isLocked()).isFalse();
    }
}
