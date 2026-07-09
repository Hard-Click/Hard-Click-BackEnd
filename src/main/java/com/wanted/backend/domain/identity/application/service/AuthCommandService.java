package com.wanted.backend.domain.identity.application.service;

import com.wanted.backend.domain.identity.application.usecase.AuthCommandUseCase;
import com.wanted.backend.domain.identity.application.usecase.EmailVerificationUseCase;
import com.wanted.backend.domain.identity.domain.model.AuthToken;
import com.wanted.backend.domain.identity.domain.model.Member;
import com.wanted.backend.domain.identity.domain.model.MemberStatus;
import com.wanted.backend.domain.identity.domain.model.RefreshToken;
import com.wanted.backend.domain.identity.domain.repository.MemberRepository;
import com.wanted.backend.domain.identity.domain.repository.RefreshTokenRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import com.wanted.backend.global.security.jwt.JwtProvider;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthCommandService implements AuthCommandUseCase {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final ApplicationEventPublisher eventPublisher;
    private final EmailVerificationUseCase emailVerificationUseCase;
    private final MeterRegistry meterRegistry;

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public AuthToken login(String username, String rawPassword) {
        Member member = memberRepository.findByUsername(username)
                .orElseGet(() -> {
                    recordLoginResult("fail");
                    throw new BusinessException(ErrorCode.INVALID_LOGIN_INFO);
                });

        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            recordLoginResult("fail");
            throw new BusinessException(ErrorCode.WITHDRAWN_MEMBER);
        }

        if (member.isLocked()) {
            recordLoginResult("fail");
            throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_LOCKED);
        }

        boolean passwordMatched = passwordEncoder.matches(rawPassword, member.getPassword());

        if (!passwordMatched) {
            member.loginFailed(LocalDateTime.now());
            memberRepository.save(member);

            recordLoginResult("fail");

            if (member.isLocked()) {
                try {
                    emailVerificationUseCase.sendAccountLockCode(member.getEmail());
                } catch (Exception e) {
                    log.warn("계정 잠금 인증코드 발송 실패. email={}", member.getEmail(), e);
                }
                throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
            }

            throw new BusinessException(ErrorCode.INVALID_LOGIN_INFO);
        }

        member.loginSuccess(LocalDateTime.now());
        memberRepository.save(member);
        member.pullDomainEvents().forEach(eventPublisher::publishEvent);

        String role = "ROLE_" + member.getRole().name();
        String accessToken = jwtProvider.createAccessToken(member.getId(), member.getUsername(), role);
        String refreshToken = jwtProvider.createRefreshToken(member.getId());

        saveRefreshToken(member.getId(), refreshToken);

        recordLoginResult("success");

        return new AuthToken(accessToken, refreshToken, member.getId(), role);
    }

    @Override
    @Transactional
    public AuthToken refresh(String refreshToken) {
        if (refreshToken == null || !jwtProvider.validateToken(refreshToken) ||
                !"refresh".equals(jwtProvider.getTokenType(refreshToken))) {
            recordReissueResult("fail");
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseGet(() -> {
                    recordReissueResult("fail");
                    throw new BusinessException(ErrorCode.UNAUTHORIZED);
                });

        if (storedToken.isExpired()) {
            refreshTokenRepository.deleteByMemberId(storedToken.getMemberId());
            recordReissueResult("fail");
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Long memberId = storedToken.getMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseGet(() -> {
                    recordReissueResult("fail");
                    throw new BusinessException(ErrorCode.USER_NOT_FOUND);
                });

        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            refreshTokenRepository.deleteByMemberId(memberId);
            recordReissueResult("fail");
            throw new BusinessException(ErrorCode.WITHDRAWN_MEMBER);
        }

        if (member.isLocked()) {
            refreshTokenRepository.deleteByMemberId(memberId);
            recordReissueResult("fail");
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        String role = "ROLE_" + member.getRole().name();
        String newAccessToken = jwtProvider.createAccessToken(memberId, member.getUsername(), role);

        recordReissueResult("success");

        return new AuthToken(newAccessToken, refreshToken, memberId, role);
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        refreshTokenRepository.deleteByMemberId(storedToken.getMemberId());
    }

    private void saveRefreshToken(Long memberId, String token) {
        LocalDateTime now = LocalDateTime.now();
        RefreshToken refreshTokenModel = new RefreshToken(
                null,
                memberId,
                token,
                now.plus(jwtProvider.getRefreshTokenExpiration()),
                now
        );
        refreshTokenRepository.save(refreshTokenModel);
    }

    private void recordLoginResult(String result) {
        Counter.builder("auth.login.count")
                .tag("result", result)
                .register(meterRegistry)
                .increment();
    }

    private void recordReissueResult(String result) {
        Counter.builder("auth.token.reissue.count")
                .tag("result", result)
                .register(meterRegistry)
                .increment();
    }
}
