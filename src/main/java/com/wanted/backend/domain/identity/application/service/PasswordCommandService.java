package com.wanted.backend.domain.identity.application.service;

import com.wanted.backend.domain.identity.application.AccountLockVerifyResult;
import com.wanted.backend.domain.identity.application.PasswordChangeResult;
import com.wanted.backend.domain.identity.application.command.AccountLockPasswordChangeCommand;
import com.wanted.backend.domain.identity.application.command.AccountLockVerifyCommand;
import com.wanted.backend.domain.identity.application.command.ResetPasswordCommand;
import com.wanted.backend.domain.identity.application.command.UpdatePasswordCommand;
import com.wanted.backend.domain.identity.application.usecase.EmailVerificationUseCase;
import com.wanted.backend.domain.identity.application.usecase.PasswordCommandUseCase;
import com.wanted.backend.domain.identity.domain.model.EmailPurpose;
import com.wanted.backend.domain.identity.domain.model.EmailVerification;
import com.wanted.backend.domain.identity.domain.model.Member;
import com.wanted.backend.domain.identity.domain.repository.EmailVerificationRepository;
import com.wanted.backend.domain.identity.domain.repository.MemberRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PasswordCommandService implements PasswordCommandUseCase {

    private final MemberRepository memberRepository;
    private final EmailVerificationRepository verificationRepository;
    private final EmailVerificationTokenCoordinator verificationTokenCoordinator;
    private final EmailVerificationUseCase emailVerificationUseCase;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public PasswordChangeResult updatePassword(Long memberId, UpdatePasswordCommand command) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(command.currentPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }

        if (!command.newPassword().equals(command.newPasswordConfirm())) {
            throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        if (passwordEncoder.matches(command.newPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_REUSE_NOT_ALLOWED);
        }

        member.changePassword(passwordEncoder.encode(command.newPassword()));
        Member saved = memberRepository.save(member);
        return PasswordChangeResult.from(saved);
    }

    @Override
    @Transactional
    public PasswordChangeResult resetPassword(ResetPasswordCommand command) {
        if (!command.newPassword().equals(command.newPasswordConfirm())) {
            throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        Member member = memberRepository.findByEmail(command.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (passwordEncoder.matches(command.newPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_REUSE_NOT_ALLOWED);
        }

        verificationTokenCoordinator.reserveForCurrentTransaction(
                command.email(),
                command.passwordChangeToken(),
                EmailPurpose.PASSWORD_RESET
        );
        member.changePasswordAndUnlock(passwordEncoder.encode(command.newPassword()), LocalDateTime.now());

        Member saved = memberRepository.save(member);
        return PasswordChangeResult.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public void verifyCurrentPassword(Long memberId, String currentPassword) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(currentPassword, member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }
    }

    @Override
    @Transactional
    public AccountLockVerifyResult verify(AccountLockVerifyCommand command) {
        Member member = memberRepository.findByEmail(command.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!member.isLocked()) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_LOCKED);
        }

        String token = emailVerificationUseCase.verifyCode(
                command.email(),
                command.code(),
                EmailPurpose.ACCOUNT_LOCK
        );
        return AccountLockVerifyResult.from(token);
    }

    @Override
    @Transactional
    public PasswordChangeResult changePassword(AccountLockPasswordChangeCommand command) {
        if (!command.newPassword().equals(command.newPasswordConfirm())) {
            throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        EmailVerification verification = verificationRepository
                .findByVerificationTokenAndPurpose(command.passwordChangeToken(), EmailPurpose.ACCOUNT_LOCK)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        Member member = memberRepository.findByEmail(verification.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!member.isLocked()) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_LOCKED);
        }

        if (passwordEncoder.matches(command.newPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_REUSE_NOT_ALLOWED);
        }

        verificationTokenCoordinator.reserveForCurrentTransaction(
                verification.getEmail(),
                command.passwordChangeToken(),
                EmailPurpose.ACCOUNT_LOCK
        );

        member.changePasswordAndUnlock(
                passwordEncoder.encode(command.newPassword()),
                LocalDateTime.now()
        );

        Member saved = memberRepository.save(member);
        return PasswordChangeResult.from(saved);
    }
}
