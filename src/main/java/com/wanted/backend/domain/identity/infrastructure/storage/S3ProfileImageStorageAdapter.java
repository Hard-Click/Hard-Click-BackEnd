package com.wanted.backend.domain.identity.infrastructure.storage;

import com.wanted.backend.domain.identity.application.port.ProfileImageStoragePort;
import com.wanted.backend.global.config.S3UrlPresigner;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class S3ProfileImageStorageAdapter implements ProfileImageStoragePort {

    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png");

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${identity.profile-image.max-size}")
    private long maxFileSize;

    private final S3Client s3Client;
    private final S3UrlPresigner s3UrlPresigner;

    public S3ProfileImageStorageAdapter(S3Client s3Client, S3UrlPresigner s3UrlPresigner) {
        this.s3Client = s3Client;
        this.s3UrlPresigner = s3UrlPresigner;
    }

    /**
     * S3에 업로드하고 DB 저장용 key를 반환한다(만료되는 URL 저장 금지).
     */
    @Override
    public String store(MultipartFile file) {
        validate(file);

        String extension = getExtension(file.getOriginalFilename());
        String key = "profiles/" + UUID.randomUUID() + "." + extension;

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .contentLength(file.getSize())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );

        } catch (S3Exception e) {
            log.error("S3 upload failed: status={}, errorCode={}, message={}, requestId={}",
                    e.statusCode(),
                    e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "N/A",
                    e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : "N/A",
                    e.requestId(), e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, e);

        } catch (IOException e) {
            log.error("IOException during S3 upload", e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, e);
        }

        return key;
    }

    /**
     * 저장된 key를 공개 조회 URL로 변환한다(응답 직전 호출). 만료가 없어 STS 토큰 수명과
     * presigned URL 유효기간 불일치로 인한 ExpiredToken 문제가 발생하지 않는다.
     */
    @Override
    public String publicUrl(String key) {
        return s3UrlPresigner.publicUrl(key);
    }

    @Override
    public void delete(String key) {
        try {
            String resolvedKey = s3UrlPresigner.extractKey(key);
            s3Client.deleteObject(r -> r.bucket(bucket).key(resolvedKey));
        } catch (Exception e) {
            log.warn("S3 프로필 이미지 삭제 실패: {}", key, e);
        }
    }

    private void validate(MultipartFile file) {
        if (file.getSize() > maxFileSize) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        String extension = getExtension(file.getOriginalFilename());

        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
        }

        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}