package com.wanted.backend.domain.study.infrastructure.persistence;

import com.wanted.backend.domain.study.domain.model.StudyParticipant;
import com.wanted.backend.domain.study.domain.repository.StudyParticipantRepository;
import org.springframework.stereotype.Repository;

@Repository
public class StudyParticipantRepositoryAdapter implements StudyParticipantRepository {

    private final SpringDataStudyParticipantRepository repository;

    public StudyParticipantRepositoryAdapter(SpringDataStudyParticipantRepository repository) {
        this.repository = repository;
    }

    @Override
    public StudyParticipant save(StudyParticipant studyParticipant) {
        StudyParticipantJpaEntity entity = new StudyParticipantJpaEntity(
                studyParticipant.getStudyId(), studyParticipant.getMemberId(), studyParticipant.getJoinedAt()
        );
        StudyParticipantJpaEntity saved = repository.save(entity);
        return StudyParticipant.restore(saved.getId(), saved.getStudyId(), saved.getMemberId(), saved.getJoinedAt());
    }
}
