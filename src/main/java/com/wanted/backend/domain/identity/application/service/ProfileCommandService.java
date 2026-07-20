package com.wanted.backend.domain.identity.application.service;

import com.wanted.backend.domain.identity.application.command.UpdateMyProfileCommand;
import com.wanted.backend.domain.identity.application.command.WithdrawMemberCommand;
import com.wanted.backend.domain.identity.application.port.ProfileImageStoragePort;
import com.wanted.backend.domain.identity.application.usecase.ProfileCommandUseCase;
import com.wanted.backend.domain.identity.domain.model.Member;
import com.wanted.backend.domain.identity.domain.model.MemberStatus;
import com.wanted.backend.domain.identity.domain.policy.PasswordPolicy;
import com.wanted.backend.domain.identity.domain.repository.MemberRepository;
import com.wanted.backend.domain.identity.domain.repository.RefreshTokenRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProfileCommandService implements ProfileCommandUseCase {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProfileImageStoragePort profileImageStoragePort;

    @Override
    public MyProfileUpdateView handle(UpdateMyProfileCommand command) {
        Member member = memberRepository.findById(command.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateHasUpdatableValue(command);

        String oldProfileImageKey = member.getProfileImageUrl();
        String profileImageKey = storeProfileImage(command.profileImage());
        String encodedPassword = resolveEncodedPassword(command, member);

        member.updateProfile(profileImageKey, encodedPassword, LocalDateTime.now());
        Member saved = memberRepository.save(member);

        // 새 프로필 이미지로 교체된 경우, 더 이상 참조되지 않는 이전 이미지를 S3에서 정리한다
        // (공개 URL이라 만료가 없어 방치하면 영구적으로 접근 가능한 상태로 남음)
        if (profileImageKey != null && oldProfileImageKey != null
                && !oldProfileImageKey.startsWith("http")) {
            profileImageStoragePort.delete(oldProfileImageKey);
        }

        String profileImageUrl = null;
        if (saved.getProfileImageUrl() != null) {
            try {
                profileImageUrl = profileImageStoragePort.publicUrl(saved.getProfileImageUrl());
            } catch (Exception e) {
                log.error("[PROFILE_PRESIGN_FAILED] 업로드 후 publicUrl 실패. key={}", saved.getProfileImageUrl(), e);
            }
        }

        return new MyProfileUpdateView(
                saved.getId(),
                saved.getName(),
                saved.getEmail(),
                profileImageUrl
        );
    }

    @Override
    public void withdraw(Long memberId, WithdrawMemberCommand command) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new BusinessException(ErrorCode.ALREADY_WITHDRAWN_MEMBER);
        }

        if (!passwordEncoder.matches(command.currentPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }

        String encodedDeletedPassword = passwordEncoder.encode(UUID.randomUUID().toString());
        member.withdraw(encodedDeletedPassword, LocalDateTime.now());
        memberRepository.save(member);

        refreshTokenRepository.deleteByMemberId(memberId);
    }

    private String storeProfileImage(MultipartFile profileImage) {
        if (profileImage == null || profileImage.isEmpty()) {
            return null;
        }
        return profileImageStoragePort.store(profileImage);
    }

    private void validateHasUpdatableValue(UpdateMyProfileCommand command) {
        boolean hasProfileImage = command.profileImage() != null && !command.profileImage().isEmpty();
        boolean hasPasswordInput = StringUtils.hasText(command.currentPassword())
                || StringUtils.hasText(command.newPassword())
                || StringUtils.hasText(command.newPasswordConfirm());

        if (!hasProfileImage && !hasPasswordInput) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private String resolveEncodedPassword(UpdateMyProfileCommand command, Member member) {
        boolean hasCurrentPassword = StringUtils.hasText(command.currentPassword());
        boolean hasNewPassword = StringUtils.hasText(command.newPassword());
        boolean hasNewPasswordConfirm = StringUtils.hasText(command.newPasswordConfirm());

        if (!hasCurrentPassword && !hasNewPassword && !hasNewPasswordConfirm) {
            return null;
        }
        if (!hasCurrentPassword || !hasNewPassword || !hasNewPasswordConfirm) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (!command.newPassword().equals(command.newPasswordConfirm())) {
            throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }
        if (!PasswordPolicy.isValid(command.newPassword())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (!passwordEncoder.matches(command.currentPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }

        return passwordEncoder.encode(command.newPassword());
    }
}
