package com.wanted.backend.domain.identity.application.service;

import com.wanted.backend.domain.identity.application.port.ProfileImageStoragePort;
import com.wanted.backend.domain.identity.application.usecase.PreSignupImageUploadUseCase.UploadedImageView;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PreSignupImageUploadServiceTest {

    private ProfileImageStoragePort profileImageStoragePort;
    private PreSignupImageUploadService service;

    @BeforeEach
    void setUp() {
        profileImageStoragePort = mock(ProfileImageStoragePort.class);
        service = new PreSignupImageUploadService(profileImageStoragePort);
    }

    @Test
    void 이미지를_업로드하면_저장key와_미리보기URL을_반환한다() {
        MockMultipartFile image = new MockMultipartFile(
                "profileImage", "profile.png", "image/png", "image".getBytes()
        );
        when(profileImageStoragePort.store(image)).thenReturn("profiles/uuid.png");
        when(profileImageStoragePort.publicUrl("profiles/uuid.png"))
                .thenReturn("https://bucket.s3/profiles/uuid.png");

        UploadedImageView result = service.upload(image);

        assertThat(result.key()).isEqualTo("profiles/uuid.png");
        assertThat(result.previewUrl()).isEqualTo("https://bucket.s3/profiles/uuid.png");
        verify(profileImageStoragePort).store(image);
    }

    @Test
    void 파일이_null이면_예외가_발생하고_저장하지_않는다() {
        assertThatThrownBy(() -> service.upload(null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);

        verify(profileImageStoragePort, never()).store(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 파일이_비어있으면_예외가_발생하고_저장하지_않는다() {
        MockMultipartFile empty = new MockMultipartFile(
                "profileImage", "profile.png", "image/png", new byte[0]
        );

        assertThatThrownBy(() -> service.upload(empty))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);

        verify(profileImageStoragePort, never()).store(org.mockito.ArgumentMatchers.any());
    }
}
