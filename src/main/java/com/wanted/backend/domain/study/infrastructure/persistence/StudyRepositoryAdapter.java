package com.wanted.backend.domain.study.infrastructure.persistence;

import com.wanted.backend.domain.study.domain.model.Study;
import com.wanted.backend.domain.study.domain.model.StudyStatus;
import com.wanted.backend.domain.study.domain.repository.StudyRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
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
                study.getId(), study.getHostId(), study.getTitle(), study.getSubject(), study.getContent(),
                study.getMaxCount(), study.getCurrentCount(), study.getStatus(),
                study.getCreatedAt(), study.getUpdatedAt()
        );
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<Study> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Study> findByIdForUpdate(Long id) {
        return repository.findByIdForUpdate(id).map(this::toDomain);
    }

    // 모집 목록/카운트는 해산(DISSOLVED)된 스터디를 제외한다(#586).
    @Override
    public List<Study> findAll(String subject, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        List<StudyJpaEntity> entities = subject != null
                ? repository.findBySubjectAndStatusNot(subject, StudyStatus.DISSOLVED, pageable)
                : repository.findByStatusNot(StudyStatus.DISSOLVED, pageable);
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public int countAll(String subject) {
        return subject != null
                ? repository.countBySubjectAndStatusNot(subject, StudyStatus.DISSOLVED)
                : repository.countByStatusNot(StudyStatus.DISSOLVED);
    }

    private Study toDomain(StudyJpaEntity entity) {
        return Study.restore(
                entity.getId(), entity.getHostId(), entity.getTitle(), entity.getSubject(),
                entity.getContent(), entity.getMaxCount(), entity.getCurrentCount(),
                entity.getStatus(), entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }
}
