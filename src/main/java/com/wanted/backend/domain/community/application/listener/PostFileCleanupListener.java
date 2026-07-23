package com.wanted.backend.domain.community.application.listener;

import com.wanted.backend.domain.community.application.port.CommunityFileStoragePort;
import com.wanted.backend.domain.community.domain.event.PostFilesDeletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 게시글 첨부파일 DB row 삭제가 커밋된 뒤 S3 오브젝트를 정리한다.
 *
 * 커밋 이후(AFTER_COMMIT) 삭제하는 이유:
 *  - 트랜잭션 내에서 S3를 지우면 이후 DB 커밋이 실패해도 S3만 삭제돼 S3/DB가 어긋난다.
 *    DB row 삭제가 커밋된 뒤에만 S3를 지워 "DB에 없으면 S3에도 없다"를 보장한다.
 *  - S3 삭제 실패는 예외를 전파하지 않고 로그로만 남긴다. DB는 이미 커밋됐으므로 여기서
 *    예외를 던지면 커밋된 요청이 500으로 보이고, 남은 S3 오브젝트는 고아로 별도 정리 대상이다.
 */
@Slf4j
@Component
public class PostFileCleanupListener {

    private final CommunityFileStoragePort storagePort;

    public PostFileCleanupListener(CommunityFileStoragePort storagePort) {
        this.storagePort = storagePort;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostFilesDeleted(PostFilesDeletedEvent event) {
        for (String fileUrl : event.fileUrls()) {
            try {
                storagePort.delete(fileUrl);
            } catch (RuntimeException e) {
                log.error("[Community] 게시글 첨부파일 S3 삭제 실패(고아 오브젝트 잔존). fileUrl={}", fileUrl, e);
            }
        }
    }
}
