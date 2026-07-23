package com.wanted.backend.domain.community.application.service;

import com.wanted.backend.domain.community.application.command.UpdatePostCommand;
import com.wanted.backend.domain.community.application.policy.CommunityAccessPolicy;
import com.wanted.backend.domain.community.application.port.CommunityFileStoragePort;
import com.wanted.backend.domain.community.domain.model.BoardType;
import com.wanted.backend.domain.community.domain.model.Post;
import com.wanted.backend.domain.community.domain.model.PostFile;
import com.wanted.backend.domain.community.domain.model.PostStatus;
import com.wanted.backend.domain.community.domain.repository.PostFileRepository;
import com.wanted.backend.domain.community.domain.repository.PostRepository;
import com.wanted.backend.domain.notification.domain.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

// 게시글 수정 시 첨부파일 처리 회귀 테스트 (버그: 신규 업로드까지 전량 삭제됨)
// 핵심: 신규 파일은 저장 후 보존되고, 기존 파일만 id 지정으로 삭제되어야 한다.
@ExtendWith(MockitoExtension.class)
class PostCommandServiceUpdateFileTest {

    @InjectMocks
    private PostCommandService postCommandService;

    @Mock
    private CommunityFileStoragePort storagePort;

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostFileRepository postFileRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private CommunityAccessPolicy communityAccessPolicy;

    private static final Long MEMBER_ID = 1L;
    private static final Long POST_ID = 100L;

    private Post existingPost() {
        return Post.restore(
                POST_ID, MEMBER_ID, BoardType.FREE, null,
                "기존 제목", "기존 내용", 0, PostStatus.ACTIVE, false,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("이미지 재첨부 수정 시 신규 파일은 저장·보존되고 기존 파일만 지정 삭제된다")
    void update_withNewFiles_keepsNewFiles_deletesOnlyOldFiles() {
        // given: 기존 첨부 2장(id 10, 11), 신규 첨부 2장 재업로드
        Post post = existingPost();
        List<PostFile> oldFiles = List.of(
                PostFile.restore(10L, POST_ID, "https://s3/posts/old-1.png", 1),
                PostFile.restore(11L, POST_ID, "https://s3/posts/old-2.png", 2)
        );
        List<MultipartFile> newFiles = List.of(
                new MockMultipartFile("files", "new-1.png", "image/png", new byte[]{1}),
                new MockMultipartFile("files", "new-2.png", "image/png", new byte[]{2})
        );
        UpdatePostCommand command = new UpdatePostCommand(
                MEMBER_ID, POST_ID, null, "새 제목", "새 내용", newFiles);

        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(postRepository.save(any(Post.class))).willReturn(post);
        given(postFileRepository.findByPostId(POST_ID)).willReturn(oldFiles);
        given(storagePort.store(any(MultipartFile.class), anyString(), anyLong()))
                .willReturn("https://s3/posts/new-1.png", "https://s3/posts/new-2.png");

        // when
        postCommandService.update(command);

        // then: 신규 파일 2건이 DB에 저장됨 (전량 삭제 버그면 커밋 후 사라짐)
        ArgumentCaptor<PostFile> savedCaptor = ArgumentCaptor.forClass(PostFile.class);
        verify(postFileRepository, times(2)).save(savedCaptor.capture());
        assertThat(savedCaptor.getAllValues())
                .extracting(PostFile::getFileUrl)
                .containsExactly("https://s3/posts/new-1.png", "https://s3/posts/new-2.png");

        // then: DB 삭제는 기존 파일 id만 대상 (post_id 전체 삭제 금지)
        verify(postFileRepository).deleteByIdIn(eq(List.of(10L, 11L)));
        verify(postFileRepository, never()).deleteByPostId(anyLong());

        // then: S3 삭제도 기존 URL만 (신규 URL은 보존)
        verify(storagePort).delete("https://s3/posts/old-1.png");
        verify(storagePort).delete("https://s3/posts/old-2.png");
        verify(storagePort, never()).delete("https://s3/posts/new-1.png");
        verify(storagePort, never()).delete("https://s3/posts/new-2.png");
    }

    @Test
    @DisplayName("파일 미첨부 수정 시 신규 업로드 없이 기존 파일만 제거된다")
    void update_withoutFiles_deletesOldFilesOnly() {
        // given: 기존 첨부 1장, 신규 파일 없음
        Post post = existingPost();
        List<PostFile> oldFiles = List.of(
                PostFile.restore(20L, POST_ID, "https://s3/posts/old.png", 1)
        );
        UpdatePostCommand command = new UpdatePostCommand(
                MEMBER_ID, POST_ID, null, "새 제목", "새 내용", null);

        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(postRepository.save(any(Post.class))).willReturn(post);
        given(postFileRepository.findByPostId(POST_ID)).willReturn(oldFiles);

        // when
        postCommandService.update(command);

        // then: 업로드/저장 호출 없음, 기존 파일만 지정 삭제
        verify(postFileRepository, never()).save(any());
        verify(storagePort, never()).store(any(), anyString(), anyLong());
        verify(postFileRepository).deleteByIdIn(eq(List.of(20L)));
        verify(postFileRepository, never()).deleteByPostId(anyLong());
        verify(storagePort).delete("https://s3/posts/old.png");
    }
}
