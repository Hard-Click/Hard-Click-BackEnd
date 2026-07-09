package com.wanted.backend.domain.identity.application.service;

import com.wanted.backend.domain.identity.application.usecase.EmailVerificationUseCase;
import com.wanted.backend.domain.identity.domain.model.Member;
import com.wanted.backend.domain.identity.domain.model.MemberStatus;
import com.wanted.backend.domain.identity.domain.model.Role;
import com.wanted.backend.domain.identity.domain.repository.MemberRepository;
import com.wanted.backend.domain.identity.domain.repository.RefreshTokenRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import com.wanted.backend.global.security.jwt.JwtProvider;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthCommandServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private EmailVerificationUseCase emailVerificationUseCase;

    private AuthCommandService authCommandService;

    @BeforeEach
    void setUp() {
        authCommandService = new AuthCommandService(
                memberRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtProvider,
                eventPublisher,
                emailVerificationUseCase,
                new SimpleMeterRegistry()
        );
    }

    private Member memberWithFailCount(int failCount) {
        LocalDateTime now = LocalDateTime.now();
        return Member.restore(
                1L, "admin_demo", "admin_demo@gmail.com", "encoded", "관리자", "M",
                null, null, null, Role.ADMIN, MemberStatus.ACTIVE, false,
                failCount, false, null, null, now, now, true
        );
    }

    @Test
    @DisplayName("5회째 실패로 잠기면 ACCOUNT_LOCKED(423)를 던지고 인증코드 발송을 시도한다")
    void login_fifthFailure_throwsAccountLocked() {
        Member member = memberWithFailCount(4);
        when(memberRepository.findByUsername("admin_demo")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> authCommandService.login("admin_demo", "wrong"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCOUNT_LOCKED);

        verify(emailVerificationUseCase).sendAccountLockCode("admin_demo@gmail.com");
    }

    @Test
    @DisplayName("인증코드 발송이 런타임 예외로 실패해도 500이 아니라 ACCOUNT_LOCKED(423)를 던진다")
    void login_fifthFailure_lockResponseIndependentOfCodeSend() {
        Member member = memberWithFailCount(4);
        when(memberRepository.findByUsername("admin_demo")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);
        doThrow(new IllegalStateException("Failed to save email verification"))
                .when(emailVerificationUseCase).sendAccountLockCode(anyString());

        assertThatThrownBy(() -> authCommandService.login("admin_demo", "wrong"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCOUNT_LOCKED);
    }
}
