package com.wanted.backend.domain.community.application.listener;

import com.wanted.backend.domain.community.application.port.CommunityFileStoragePort;
import com.wanted.backend.domain.community.domain.event.PostFilesDeletedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostFileCleanupListenerTest {

    @InjectMocks
    private PostFileCleanupListener listener;

    @Mock
    private CommunityFileStoragePort storagePort;

    @Test
    @DisplayName("이벤트의 모든 파일 URL을 S3에서 삭제한다")
    void onPostFilesDeleted_deletesAllUrls() {
        PostFilesDeletedEvent event = PostFilesDeletedEvent.of(
                List.of("https://s3/posts/a.png", "https://s3/posts/b.png"));

        listener.onPostFilesDeleted(event);

        verify(storagePort).delete("https://s3/posts/a.png");
        verify(storagePort).delete("https://s3/posts/b.png");
    }

    @Test
    @DisplayName("일부 S3 삭제가 실패해도 예외를 전파하지 않고 나머지를 계속 삭제한다")
    void onPostFilesDeleted_swallowsFailure_andContinues() {
        // 커밋 후 실행이므로 예외를 던지면 안 되고, 첫 실패가 뒤 삭제를 막아서도 안 된다
        willThrow(new RuntimeException("S3 down")).given(storagePort).delete("https://s3/posts/a.png");

        PostFilesDeletedEvent event = PostFilesDeletedEvent.of(
                List.of("https://s3/posts/a.png", "https://s3/posts/b.png"));

        assertThatCode(() -> listener.onPostFilesDeleted(event)).doesNotThrowAnyException();

        verify(storagePort).delete("https://s3/posts/b.png");
    }
}
