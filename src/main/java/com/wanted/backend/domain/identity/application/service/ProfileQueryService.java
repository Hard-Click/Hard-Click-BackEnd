package com.wanted.backend.domain.identity.application.service;

import com.wanted.backend.domain.identity.application.port.ProfileImageStoragePort;
import com.wanted.backend.domain.identity.application.usecase.ProfileQueryUseCase;
import com.wanted.backend.domain.identity.domain.model.Member;
import com.wanted.backend.domain.identity.domain.repository.MemberRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileQueryService implements ProfileQueryUseCase {

    private static final String DEFAULT_PROFILE_IMAGE_URL = "/images/default-profile.png";

    private final MemberRepository memberRepository;
    private final ProfileImageStoragePort profileImageStoragePort;

    @Override
    public MyProfileView handle(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return new MyProfileView(
                member.getId(),
                member.getName(),
                member.getEmail(),
                resolveProfileImageUrl(member.getProfileImageUrl())
        );
    }

    private String resolveProfileImageUrl(String profileImageKey) {
        if (profileImageKey == null || profileImageKey.isBlank()) {
            return DEFAULT_PROFILE_IMAGE_URL;
        }
        try {
            return profileImageStoragePort.publicUrl(profileImageKey);
        } catch (Exception e) {
            log.error("[PROFILE_PRESIGN_FAILED] publicUrl 실패, 기본 이미지로 대체. key={}", profileImageKey, e);
            return DEFAULT_PROFILE_IMAGE_URL;
        }
    }
}
