package com.wanted.backend.domain.community.infrastructure.persistence;

import com.wanted.backend.domain.community.domain.model.PostFile;
import com.wanted.backend.domain.community.domain.repository.PostFileRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PostFileRepositoryAdapter implements PostFileRepository {

    private final SpringDataPostFileRepository repository;

    public PostFileRepositoryAdapter(SpringDataPostFileRepository repository) {
        this.repository = repository;
    }

    @Override
    public PostFile save(PostFile postFile) {
        PostFileJpaEntity entity = new PostFileJpaEntity(
                postFile.getPostId(),
                postFile.getFileUrl(),
                postFile.getSortOrder()
        );
        return toDomain(repository.save(entity));
    }

    private PostFile toDomain(PostFileJpaEntity entity) {
        return PostFile.restore(
                entity.getId(),
                entity.getPostId(),
                entity.getFileUrl(),
                entity.getSortOrder()
        );
    }


    @Override
    public List<PostFile> findByPostId(Long postId) {
        return repository.findByPostIdOrderBySortOrderAsc(postId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteByPostId(Long postId) {
        repository.deleteByPostId(postId);
    }

    @Override
    public void deleteByIdIn(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        repository.deleteByIdIn(ids);
    }

}