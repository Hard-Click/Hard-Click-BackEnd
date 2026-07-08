package com.wanted.backend.domain.study.infrastructure.persistence;

import com.wanted.backend.domain.study.domain.model.Study;
import com.wanted.backend.domain.study.domain.repository.StudyRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class StudyRepositoryAdapter implements StudyRepository {

    private final SpringDataStudyRepository repository;

    public StudyRepositoryAdapter(SpringDataStudyRepository repository) {
        this.repository = repository;
    }

    @Override
    public Study save(Study study) {
        StudyJpaEntity entity = new StudyJpaEntity(
                study.getHostId(), study.getTitle(), study.getSubject(), study.getContent(),
                study.getMaxCount(), study.getCurrentCount(), study.getStatus(),
                study.getCreatedAt(), study.getUpdatedAt()
        );
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<Study> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    private Study toDomain(StudyJpaEntity entity) {
        return Study.restore(
                entity.getId(), entity.getHostId(), entity.getTitle(), entity.getSubject(),
                entity.getContent(), entity.getMaxCount(), entity.getCurrentCount(),
                entity.getStatus(), entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }
}
