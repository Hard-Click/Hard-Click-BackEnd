package com.wanted.backend.domain.community.domain.repository;

import com.wanted.backend.domain.community.domain.model.BoardType;
import com.wanted.backend.domain.community.domain.model.Post;
import com.wanted.backend.domain.community.domain.model.PostSortType;
import com.wanted.backend.domain.community.domain.model.PostSummary;

import java.util.List;
import java.util.Optional;


public interface PostRepository {

    // 게시글 저장
    Post save(Post post);

    // 단건 조회 (상세 조회, 수정, 삭제용)
    Optional<Post> findById(Long postId);

    // 단건 조회 + 쓰기 잠금 (수정용) — 동시 PATCH의 첨부파일 개수 race를 직렬화로 방지
    Optional<Post> findByIdForUpdate(Long postId);

    // 게시판 타입별 목록 페이징 조회
    List<Post> findByBoardType(BoardType boardType, PostSortType sort,
                               String keyword, int page, int size);

    // 전체 목록 페이징 조회
    List<Post> findAll(PostSortType sort, String keyword, int page, int size);

    // 댓글순 정렬 — JOIN + DTO Projection으로 작성자명/댓글수까지 한 쿼리에 채워서 반환 (방법③)
    List<PostSummary> findSummaryByBoardTypeOrderByCommentCount(BoardType boardType, String keyword, int page, int size);

    List<PostSummary> findAllSummaryOrderByCommentCount(String keyword, int page, int size);

    // 댓글순 정렬 — comment_count 컬럼을 그대로 읽기만 함, COUNT/JOIN 없음 (방법④: 비정규화)
    List<PostSummary> findSummaryByBoardTypeOrderByCommentCountDenormalized(BoardType boardType, String keyword, int page, int size);

    List<PostSummary> findAllSummaryOrderByCommentCountDenormalized(String keyword, int page, int size);

    // 댓글 생성/삭제 시 posts.comment_count 동기화 (방법④)
    void incrementCommentCount(Long postId);

    void decrementCommentCount(Long postId);

    // 게시판 타입별 전체 수 (페이징 계산용)
    int countByBoardType(BoardType boardType, String keyword);

    // 전체 수
    int countAll(String keyword);

    // 조회수 업데이트
    void updateViewCount(Long postId, int viewCount);

    //게시글 삭제
    void deleteById(Long postId);

    void adminDeleteById(Long postId);
}
