package com.wanted.backend.domain.identity.application.service;

import com.wanted.backend.domain.identity.application.port.ProfileImageStoragePort;
import com.wanted.backend.domain.identity.application.usecase.PreSignupImageUploadUseCase;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 회원가입 전(무인증) 프로필 이미지 업로드.
 * DB 트랜잭션이 없으므로 @Transactional을 두지 않는다(외부 S3 I/O만 수행).
 * 파일 크기·확장자 검증은 {@link ProfileImageStoragePort#store}가 담당한다.
 */
@Service
@RequiredArgsConstructor
public class PreSignupImageUploadService implements PreSignupImageUploadUseCase {

    private final ProfileImageStoragePort profileImageStoragePort;

    @Override
    public UploadedImageView upload(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        String key = profileImageStoragePort.store(image);
        String previewUrl = profileImageStoragePort.publicUrl(key);

        return new UploadedImageView(key, previewUrl);
    }
}
