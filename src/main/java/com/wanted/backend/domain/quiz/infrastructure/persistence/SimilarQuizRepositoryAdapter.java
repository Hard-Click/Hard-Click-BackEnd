package com.wanted.backend.domain.quiz.infrastructure.persistence;

import com.wanted.backend.domain.quiz.domain.model.SimilarQuiz;
import com.wanted.backend.domain.quiz.domain.repository.SimilarQuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SimilarQuizRepositoryAdapter implements SimilarQuizRepository {

    private final SimilarQuizJpaRepository similarQuizJpaRepository;

    @Override
    @Transactional
    public SimilarQuiz save(SimilarQuiz similarQuiz) {
        SimilarQuizJpaEntity entity = SimilarQuizJpaEntity.create(
                similarQuiz.getMemberId(), similarQuiz.getCourseId(), similarQuiz.getWeek(),
                similarQuiz.getTitle(), similarQuiz.getCreatedAt());

        int order = 1;
        for (Long questionId : similarQuiz.getQuestionIds()) {
            entity.addQuestion(questionId, order++);
        }

        return toDomain(similarQuizJpaRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SimilarQuiz> findById(Long id) {
        return similarQuizJpaRepository.findById(id).map(this::toDomain);
    }

    private SimilarQuiz toDomain(SimilarQuizJpaEntity entity) {
        var questionIds = entity.getQuestions().stream()
                .map(SimilarQuizQuestionJpaEntity::getQuestionId)
                .toList();

        return SimilarQuiz.restore(entity.getId(), entity.getMemberId(), entity.getCourseId(),
                entity.getWeek(), entity.getTitle(), questionIds, entity.getCreatedAt());
    }
}
