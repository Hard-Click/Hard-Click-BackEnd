package com.wanted.backend.domain.identity.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "가입 전 프로필 이미지 업로드 응답")
public record PreSignupProfileImageResponse(
        @Schema(description = "가입 API(profileImageUrl)에 그대로 전달할 저장 key", example = "profiles/9f1c-uuid.png")
        String key,

        @Schema(description = "프론트 미리보기용 조회 URL", example = "https://bucket.s3.ap-northeast-2.amazonaws.com/profiles/9f1c-uuid.png")
        String previewUrl
) {
}
