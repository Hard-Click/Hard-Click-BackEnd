package com.wanted.backend.domain.study.infrastructure.persistence;

import com.wanted.backend.domain.study.domain.model.Study;
import com.wanted.backend.domain.study.domain.repository.StudyRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;

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
    public List<Study> findAll(String subject, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        List<StudyJpaEntity> entities = subject != null
                ? repository.findBySubject(subject, pageable)
                : repository.findAll(pageable).getContent();
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public int countAll(String subject) {
        return subject != null ? repository.countBySubject(subject) : (int) repository.count();
    }

    private Study toDomain(StudyJpaEntity entity) {
        return Study.restore(
                entity.getId(), entity.getHostId(), entity.getTitle(), entity.getSubject(),
                entity.getContent(), entity.getMaxCount(), entity.getCurrentCount(),
                entity.getStatus(), entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }
}
