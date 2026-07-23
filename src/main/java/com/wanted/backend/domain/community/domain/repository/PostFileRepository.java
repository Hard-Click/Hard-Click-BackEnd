package com.wanted.backend.domain.community.domain.repository;

import com.wanted.backend.domain.community.domain.model.PostFile;

import java.util.List;

public interface PostFileRepository {

    // 첨부파일 저장
    PostFile save(PostFile postFile);

    // 게시글 첨부파일 목록 조회 (sortOrder 오름차순)
    List<PostFile> findByPostId(Long postId);

    //게시글 삭제 시 첨부파일 같이 삭제
    void deleteByPostId(Long postId);

    // 지정한 첨부파일만 삭제 (게시글 수정 시 기존 파일만 제거, 신규 파일은 보존)
    void deleteByIdIn(List<Long> ids);
}