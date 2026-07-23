package com.wanted.backend.domain.community.application.service;

import com.wanted.backend.domain.community.application.command.CreatePostCommand;
import com.wanted.backend.domain.community.application.command.DeletePostCommand;
import com.wanted.backend.domain.community.application.command.UpdatePostCommand;
import com.wanted.backend.domain.community.application.policy.CommunityAccessPolicy;
import com.wanted.backend.domain.community.application.port.CommunityFileStoragePort;
import com.wanted.backend.domain.community.application.usecase.PostCommandUseCase;
import com.wanted.backend.domain.community.domain.event.PostFilesDeletedEvent;
import com.wanted.backend.domain.community.domain.model.Post;
import com.wanted.backend.domain.community.domain.model.PostFile;
import com.wanted.backend.domain.community.domain.repository.PostFileRepository;
import com.wanted.backend.domain.community.domain.repository.PostRepository;
import com.wanted.backend.domain.notification.domain.repository.NotificationRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class PostCommandService implements PostCommandUseCase {

    @Value("${community.image.max-size}")
    private long maxFileSize;

    private final CommunityFileStoragePort storagePort;
    private final PostRepository postRepository;
    private final PostFileRepository postFileRepository;
    private final NotificationRepository notificationRepository;
    private final CommunityAccessPolicy communityAccessPolicy;
    private final ApplicationEventPublisher eventPublisher;

    public PostCommandService(CommunityFileStoragePort storagePort,
                              PostRepository postRepository,
                              PostFileRepository postFileRepository, NotificationRepository notificationRepository,
                              CommunityAccessPolicy communityAccessPolicy,
                              ApplicationEventPublisher eventPublisher) {
        this.storagePort = storagePort;
        this.postRepository = postRepository;
        this.postFileRepository = postFileRepository;
        this.notificationRepository = notificationRepository;
        this.communityAccessPolicy = communityAccessPolicy;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @CacheEvict(cacheNames = "postCount:v1", allEntries = true)
    public Long create(CreatePostCommand command) {
        communityAccessPolicy.validateAccess(command.authorId());

        int fileCount = command.files() != null ? command.files().size() : 0;

        Post post = Post.create(
                command.authorId(),
                command.boardType(),
                command.subject(),
                command.title(),
                command.content(),
                fileCount
        );

        Post saved = postRepository.save(post);

        if (fileCount > 0) {
            List<String> uploadedUrls = new ArrayList<>();
            try {
                for (int i = 0; i < command.files().size(); i++) {
                    MultipartFile file = command.files().get(i);
                    String fileUrl = storagePort.store(file, "posts", maxFileSize);
                    uploadedUrls.add(fileUrl);
                    postFileRepository.save(PostFile.create(saved.getId(), fileUrl, i + 1));
                }
            } catch (Exception e) {
                uploadedUrls.forEach(storagePort::delete);
                throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, e);
            }
        }

        return saved.getId();
    }

    @Override
    @CacheEvict(cacheNames = "postCount:v1", allEntries = true)
    public void delete(DeletePostCommand command) {
        communityAccessPolicy.validateAccess(command.memberId());

        Post post = postRepository.findById(command.postId())
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (command.isAdmin()) {
            // 관리자는 소유권 검증 없이 소프트 삭제 (ADMIN_DELETED 상태로 변경)
            post.softDeleteByAdmin(LocalDateTime.now());
            postRepository.save(post);
            return;
        }

        post.validateDeletable(command.memberId());

        List<String> fileUrls = postFileRepository.findByPostId(command.postId()).stream()
                .map(PostFile::getFileUrl)
                .toList();

        postFileRepository.deleteByPostId(command.postId());
        postRepository.deleteById(command.postId());
        notificationRepository.deleteByRedirectUrlStartingWith("/community/" + command.postId());

        // S3 오브젝트 삭제는 커밋 후(AFTER_COMMIT)로 분리 — 롤백 시 S3/DB 불일치 방지
        if (!fileUrls.isEmpty()) {
            eventPublisher.publishEvent(PostFilesDeletedEvent.of(fileUrls));
        }
    }

    @Override
    public Long update(UpdatePostCommand command) {
        communityAccessPolicy.validateAccess(command.memberId());

        // [1단계] 게시글 존재 여부 확인 (+ 쓰기 잠금) — 동시 PATCH의 첨부파일 개수 race를
        // 게시글 행 잠금으로 직렬화한다. 뒤 요청은 앞 요청 커밋 후 최신 파일 상태를 읽는다.
        Post post = postRepository.findByIdForUpdate(command.postId())
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        // [2단계] 본인 게시글 여부 + 채택글 여부 검증 → 도메인이 담당
        post.validateUpdatable(command.memberId());

        // [3단계] 파일 개수 검증 → 도메인이 담당
        int fileCount = command.files() != null ? command.files().size() : 0;
        post.validateFileCount(fileCount);

        // [4단계] 게시글 값 수정 → 도메인이 담당
        post.update(command.subject(), command.title(), command.content());

        // [5단계] 변경된 게시글 DB 저장
        Post saved = postRepository.save(post);

        // [6단계] 기존 첨부파일 스냅샷 확보 (신규 업로드 전) — 7단계에서 "기존 것만" 지정 삭제하기 위함
        List<PostFile> oldFiles = postFileRepository.findByPostId(command.postId());

        // [7단계] 새 파일 S3 업로드 (파일 있을 때만) — 기존 파일 삭제 전에 먼저 업로드
        List<String> uploadedUrls = new ArrayList<>();
        if (fileCount > 0) {
            try {
                for (int i = 0; i < command.files().size(); i++) {
                    MultipartFile file = command.files().get(i);
                    String fileUrl = storagePort.store(file, "posts", maxFileSize);
                    uploadedUrls.add(fileUrl);
                    postFileRepository.save(PostFile.create(saved.getId(), fileUrl, i + 1));
                }
            } catch (Exception e) {
                uploadedUrls.forEach(storagePort::delete);
                throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, e);
            }
        }

        // [8단계] 기존 첨부파일 DB row만 삭제 — 신규 업로드 성공 후, 기존 스냅샷 대상으로만 삭제(신규 row 보존)
        postFileRepository.deleteByIdIn(oldFiles.stream().map(PostFile::getId).toList());

        // [9단계] 기존 파일 S3 삭제는 커밋 후(AFTER_COMMIT)로 분리 — 롤백 시 S3/DB 불일치 방지
        if (!oldFiles.isEmpty()) {
            eventPublisher.publishEvent(PostFilesDeletedEvent.of(
                    oldFiles.stream().map(PostFile::getFileUrl).toList()));
        }

        return saved.getId();
    }
}
