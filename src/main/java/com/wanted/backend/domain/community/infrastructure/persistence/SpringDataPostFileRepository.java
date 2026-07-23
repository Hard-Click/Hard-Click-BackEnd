package com.wanted.backend.domain.community.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataPostFileRepository
        extends JpaRepository<PostFileJpaEntity, Long> {

    List<PostFileJpaEntity> findByPostIdOrderBySortOrderAsc(Long postId);

    void deleteByPostId(Long postId);

    void deleteByIdIn(List<Long> ids);
}