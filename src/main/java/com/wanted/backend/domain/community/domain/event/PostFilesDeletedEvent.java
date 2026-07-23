package com.wanted.backend.domain.community.domain.event;

import com.wanted.backend.global.domain.DomainEvent;

import java.time.Instant;
import java.util.List;

// 게시글 첨부파일 DB row 삭제가 커밋된 뒤 S3 오브젝트를 정리하기 위해 발행.
// AFTER_COMMIT에서 S3를 삭제하므로 트랜잭션이 롤백되면 S3는 건드리지 않아 S3/DB 정합성이 유지된다.
public record PostFilesDeletedEvent(
        List<String> fileUrls,
        Instant occurredAt
) implements DomainEvent {

    public static PostFilesDeletedEvent of(List<String> fileUrls) {
        return new PostFilesDeletedEvent(List.copyOf(fileUrls), Instant.now());
    }
}
