package com.wanted.backend.domain.identity.application.service;

import com.wanted.backend.domain.identity.application.usecase.AuthCommandUseCase;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 5회째 로그인 실패로 계정이 잠기는 순간, 잠금 인증코드 발송(sendAccountLockCode)이 실패해도
 * 로그인 트랜잭션이 오염되지 않고 ACCOUNT_LOCKED(423)가 정상 전달되며 잠금이 커밋되는지
 * 실제 스프링 트랜잭션/DB로 검증한다.
 *
 * 순수 Mockito 단위 테스트로는 재현 불가능한 버그였다. 실 배포에서 데모 계정 이메일이 비-gmail이라
 * sendAccountLockCode가 INVALID_EMAIL_DOMAIN을 던졌고, 이 메서드가 로그인 트랜잭션에 참여(REQUIRED)
 * 하고 있어 공유 트랜잭션이 rollback-only로 마킹됐다. 호출부에서 예외를 catch 해도 커밋 시점에
 * UnexpectedRollbackException(500)이 터지고 잠금도 롤백돼, 5회째마다 영구 500이 반복됐다.
 * (mock은 REQUIRED 참여 트랜잭션의 rollback-only 전파를 재현하지 못해 초록으로 통과했다.)
 */
@SpringBootTest
@ActiveProfiles("test")
class AccountLockTransactionTest {

    private static final long MEMBER_ID = 999_990_100L;
    private static final String USERNAME = "lock_tx_test_user";
    // 비-gmail 도메인이라 sendAccountLockCode 내부에서 INVALID_EMAIL_DOMAIN이 던져진다.
    private static final String NON_GMAIL_EMAIL = "lock_tx_test@example.com";
    private static final String CORRECT_PASSWORD = "Correct1234!";

    @Autowired
    private AuthCommandUseCase authCommandUseCase;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        cleanUp();
        LocalDateTime now = LocalDateTime.now();
        // login_fail_count = 4 → 다음 실패 1회로 5회에 도달해 잠금이 트리거된다.
        jdbcTemplate.update(
                "INSERT INTO members (member_id, name, created_at, email, is_locked, "
                        + "is_password_change_required, login_fail_count, optional_terms_agreed, "
                        + "password, role, status, updated_at, username) "
                        + "VALUES (?, ?, ?, ?, 0, 0, 4, 1, ?, 'STUDENT', 'ACTIVE', ?, ?)",
                MEMBER_ID, "잠금테스트", now, NON_GMAIL_EMAIL,
                passwordEncoder.encode(CORRECT_PASSWORD), now, USERNAME
        );
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        jdbcTemplate.update("DELETE FROM members WHERE member_id = ?", MEMBER_ID);
    }

    @Test
    @DisplayName("5회째 실패 시 잠금코드 발송이 실패(비-gmail)해도 500이 아니라 ACCOUNT_LOCKED(423)를 던지고 잠금이 커밋된다")
    void login_fifthFailure_lockCommitsEvenWhenCodeSendFails() {
        assertThatThrownBy(() -> authCommandUseCase.login(USERNAME, "wrong-password"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCOUNT_LOCKED);

        // 로그인 트랜잭션이 롤백되지 않고 잠금 상태가 실제로 커밋됐는지 확인한다.
        Boolean isLocked = jdbcTemplate.queryForObject(
                "SELECT is_locked FROM members WHERE member_id = ?", Boolean.class, MEMBER_ID);
        Integer failCount = jdbcTemplate.queryForObject(
                "SELECT login_fail_count FROM members WHERE member_id = ?", Integer.class, MEMBER_ID);

        assertThat(isLocked).isTrue();
        assertThat(failCount).isEqualTo(5);
    }

    @Test
    @DisplayName("이미 잠긴 계정으로 로그인하면 500이 아니라 ACCOUNT_ALREADY_LOCKED(403)를 던진다")
    void login_alreadyLocked_throwsAlreadyLocked() {
        jdbcTemplate.update(
                "UPDATE members SET is_locked = 1, login_fail_count = 5, locked_at = ? WHERE member_id = ?",
                LocalDateTime.now(), MEMBER_ID);

        assertThatThrownBy(() -> authCommandUseCase.login(USERNAME, "wrong-password"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCOUNT_ALREADY_LOCKED);
    }
}
