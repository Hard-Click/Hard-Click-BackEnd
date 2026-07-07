package com.wanted.backend.domain.community.application.service;

import com.wanted.backend.domain.community.application.command.CreatePostCommand;
import com.wanted.backend.domain.community.application.command.DeletePostCommand;
import com.wanted.backend.domain.community.application.command.UpdatePostCommand;
import com.wanted.backend.domain.community.application.policy.CommunityAccessPolicy;
import com.wanted.backend.domain.community.application.port.CommunityFileStoragePort;
import com.wanted.backend.domain.community.application.usecase.PostCommandUseCase;
import com.wanted.backend.domain.community.domain.model.Post;
import com.wanted.backend.domain.community.domain.model.PostFile;
import com.wanted.backend.domain.community.domain.repository.PostFileRepository;
import com.wanted.backend.domain.community.domain.repository.PostRepository;
import com.wanted.backend.domain.notification.domain.repository.NotificationRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
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

    public PostCommandService(CommunityFileStoragePort storagePort,
                              PostRepository postRepository,
                              PostFileRepository postFileRepository, NotificationRepository notificationRepository,
                              CommunityAccessPolicy communityAccessPolicy) {
        this.storagePort = storagePort;
        this.postRepository = postRepository;
        this.postFileRepository = postFileRepository;
        this.notificationRepository = notificationRepository;
        this.communityAccessPolicy = communityAccessPolicy;
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

        postFileRepository.findByPostId(command.postId())
                .forEach(file -> storagePort.delete(file.getFileUrl()));

        postFileRepository.deleteByPostId(command.postId());
        postRepository.deleteById(command.postId());
        notificationRepository.deleteByRedirectUrlStartingWith("/community/" + command.postId());
    }

    @Override
    public Long update(UpdatePostCommand command) {
        communityAccessPolicy.validateAccess(command.memberId());

        // [1단계] 게시글 존재 여부 확인
        Post post = postRepository.findById(command.postId())
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

        // [6단계] 새 파일 S3 업로드 (파일 있을 때만) — 기존 파일 삭제 전에 먼저 업로드
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

        // [7단계] 기존 첨부파일 S3 삭제 및 DB 삭제 — 새 업로드 성공 후에 삭제
        postFileRepository.findByPostId(command.postId()).stream()
                .filter(file -> !uploadedUrls.contains(file.getFileUrl()))
                .forEach(file -> storagePort.delete(file.getFileUrl()));
        postFileRepository.deleteByPostId(command.postId());

        return saved.getId();
    }
}
