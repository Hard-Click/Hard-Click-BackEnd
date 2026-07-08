package com.wanted.backend.domain.identity.application.service;

import com.wanted.backend.domain.identity.application.command.AccountLockPasswordChangeCommand;
import com.wanted.backend.domain.identity.application.command.ResetPasswordCommand;
import com.wanted.backend.domain.identity.application.command.UpdatePasswordCommand;
import com.wanted.backend.domain.identity.application.usecase.EmailVerificationUseCase;
import com.wanted.backend.domain.identity.domain.model.EmailPurpose;
import com.wanted.backend.domain.identity.domain.model.EmailVerification;
import com.wanted.backend.domain.identity.domain.model.Member;
import com.wanted.backend.domain.identity.domain.repository.EmailVerificationRepository;
import com.wanted.backend.domain.identity.domain.repository.MemberRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("기존 비밀번호와 동일한 값으로는 비밀번호를 변경할 수 없다")
class PasswordReuseTest {

    @InjectMocks
    private PasswordCommandService passwordCommandService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private EmailVerificationRepository verificationRepository;

    @Mock
    private EmailVerificationTokenCoordinator verificationTokenCoordinator;

    @Mock
    private EmailVerificationUseCase emailVerificationUseCase;

    @Mock
    private PasswordEncoder passwordEncoder;

    private static final String ENCODED = "$2a$10$encoded";

    @Test
    @DisplayName("updatePassword: 새 비밀번호가 기존 비밀번호와 같으면 예외")
    void updatePassword_reuse_throws() {
        Member member = mock(Member.class);
        when(member.getPassword()).thenReturn(ENCODED);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        // 현재 비밀번호 검증 통과 + 재사용 검증 모두 true
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        UpdatePasswordCommand command =
                new UpdatePasswordCommand("Test1234!", "Test1234!", "Test1234!");

        assertThatThrownBy(() -> passwordCommandService.updatePassword(1L, command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PASSWORD_REUSE_NOT_ALLOWED);
    }

    @Test
    @DisplayName("resetPassword: 새 비밀번호가 기존 비밀번호와 같으면 예외")
    void resetPassword_reuse_throws() {
        Member member = mock(Member.class);
        when(member.getPassword()).thenReturn(ENCODED);
        when(memberRepository.findByEmail("user@gmail.com")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("Test1234!", ENCODED)).thenReturn(true);

        ResetPasswordCommand command =
                new ResetPasswordCommand("user@gmail.com", "token", "Test1234!", "Test1234!");

        assertThatThrownBy(() -> passwordCommandService.resetPassword(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PASSWORD_REUSE_NOT_ALLOWED);
    }

    @Test
    @DisplayName("changePassword(계정잠금 해제): 새 비밀번호가 기존 비밀번호와 같으면 예외")
    void changePassword_reuse_throws() {
        EmailVerification verification = mock(EmailVerification.class);
        when(verification.getEmail()).thenReturn("user@gmail.com");
        Member member = mock(Member.class);
        when(member.getPassword()).thenReturn(ENCODED);
        when(member.isLocked()).thenReturn(true);
        when(verificationRepository.findByVerificationTokenAndPurpose("token", EmailPurpose.ACCOUNT_LOCK))
                .thenReturn(Optional.of(verification));
        when(memberRepository.findByEmail("user@gmail.com")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("Test1234!", ENCODED)).thenReturn(true);

        AccountLockPasswordChangeCommand command =
                new AccountLockPasswordChangeCommand("token", "Test1234!", "Test1234!");

        assertThatThrownBy(() -> passwordCommandService.changePassword(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PASSWORD_REUSE_NOT_ALLOWED);
    }
}
