package com.wanted.backend.domain.identity.application.service;

import com.wanted.backend.domain.identity.application.port.EmailSendPort;
import com.wanted.backend.domain.identity.application.usecase.EmailVerificationUseCase;
import com.wanted.backend.domain.identity.domain.model.EmailPurpose;
import com.wanted.backend.domain.identity.domain.model.EmailVerification;
import com.wanted.backend.domain.identity.domain.policy.EmailPolicy;
import com.wanted.backend.domain.identity.domain.repository.EmailVerificationRepository;
import com.wanted.backend.domain.identity.domain.repository.MemberRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

@Service
public class EmailVerificationService implements EmailVerificationUseCase {

    private static final ZoneId DAILY_LIMIT_ZONE = ZoneId.of("Asia/Seoul");

    private final EmailVerificationRepository verificationRepository;
    private final EmailSendPort emailSendPort;
    private final MemberRepository memberRepository;
    private final Duration codeTtl;
    private final String allowedEmailDomain;
    private final int passwordResetDailyLimit;

    public EmailVerificationService(
            EmailVerificationRepository verificationRepository,
            EmailSendPort emailSendPort,
            MemberRepository memberRepository,
            @Value("${identity.email.code-ttl}") Duration codeTtl,
            @Value("${identity.email.allowed-domain}") String allowedEmailDomain,
            @Value("${identity.email.password-reset-daily-limit}") int passwordResetDailyLimit
    ) {
        this.verificationRepository = verificationRepository;
        this.emailSendPort = emailSendPort;
        this.memberRepository = memberRepository;
        this.codeTtl = codeTtl;
        this.allowedEmailDomain = allowedEmailDomain;
        this.passwordResetDailyLimit = passwordResetDailyLimit;
    }

    @Override
    @Transactional
    public void sendVerificationCode(String email, EmailPurpose purpose) {
        if (!EmailPolicy.isAllowedDomain(email, allowedEmailDomain)) {
            throw new BusinessException(ErrorCode.INVALID_EMAIL_DOMAIN);
        }
        verificationRepository.revokeActiveByEmailAndPurpose(email, purpose);

        EmailVerification verification = EmailVerification.create(email, purpose, codeTtl);
        verificationRepository.save(verification);
        sendVerificationCodeAfterCommit(email, verification.getCode());
    }

    @Override
    @Transactional
    public void sendPasswordResetCode(String email) {
        if (!memberRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        LocalDateTime tomorrow = LocalDateTime.of(
                LocalDateTime.now(DAILY_LIMIT_ZONE).toLocalDate().plusDays(1),
                LocalTime.MIN
        );
        if (!verificationRepository.tryAcquireSendPermission(
                email,
                EmailPurpose.PASSWORD_RESET,
                passwordResetDailyLimit,
                tomorrow
        )) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_LIMIT_EXCEEDED);
        }

        sendVerificationCode(email, EmailPurpose.PASSWORD_RESET);
    }

    @Override
    @Transactional
    public String verifyCode(String email, String code, EmailPurpose purpose) {
        EmailVerification verification = verificationRepository
                .findLatestPendingByEmailAndPurpose(email, purpose)
                .orElseThrow(() -> new BusinessException(ErrorCode.VERIFICATION_NOT_FOUND));

        verification.verify(code);

        verificationRepository.save(verification);
        return verification.getVerificationToken();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendAccountLockCode(String email) {
        sendVerificationCode(email, EmailPurpose.ACCOUNT_LOCK);
    }

    private void sendVerificationCodeAfterCommit(String email, String code) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            emailSendPort.sendVerificationCode(email, code);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                emailSendPort.sendVerificationCode(email, code);
            }
        });
    }
}
