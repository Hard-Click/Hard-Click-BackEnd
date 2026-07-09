package com.wanted.backend.domain.study.infrastructure.persistence;

import com.wanted.backend.domain.study.domain.model.StudyParticipant;
import com.wanted.backend.domain.study.domain.repository.StudyParticipantRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

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

    @Override
    public List<Long> findMemberIdsByStudyId(Long studyId) {
        return repository.findByStudyId(studyId).stream()
                .map(StudyParticipantJpaEntity::getMemberId)
                .toList();
    }

    @Override
    public boolean existsByStudyIdAndMemberId(Long studyId, Long memberId) {
        return repository.existsByStudyIdAndMemberId(studyId, memberId);
    }
}
