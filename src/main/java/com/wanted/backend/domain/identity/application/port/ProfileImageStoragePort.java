package com.wanted.backend.domain.identity.application.port;

import org.springframework.web.multipart.MultipartFile;

public interface ProfileImageStoragePort {

    String store(MultipartFile file);

    /** 저장된 S3 key를 조회용 공개 URL로 변환한다(서명 없음, 버킷이 해당 prefix를 public read로 열어둬야 함). */
    String publicUrl(String key);

    /** 더 이상 참조되지 않는 프로필 이미지를 S3에서 삭제한다. */
    void delete(String key);
}
