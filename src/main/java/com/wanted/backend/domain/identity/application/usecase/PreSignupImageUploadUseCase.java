package com.wanted.backend.domain.identity.application.usecase;

import org.springframework.web.multipart.MultipartFile;

public interface PreSignupImageUploadUseCase {

    /**
     * 회원가입 전(로그인 없이) 프로필 이미지를 업로드한다.
     * DB에 저장할 key와 미리보기용 조회 URL을 함께 반환한다.
     */
    UploadedImageView upload(MultipartFile image);

    /**
     * @param key        가입 API의 profileImageUrl로 그대로 전달할 S3 저장 key
     * @param previewUrl 프론트 미리보기용 조회 URL(만료 없음)
     */
    record UploadedImageView(String key, String previewUrl) {
    }
}
