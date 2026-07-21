package com.wanted.backend.domain.quiz.infrastructure.persistence;

import com.wanted.backend.domain.quiz.domain.model.SimilarQuizSubmission;
import com.wanted.backend.domain.quiz.domain.repository.SimilarQuizSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SimilarQuizSubmissionRepositoryAdapter implements SimilarQuizSubmissionRepository {

    private final SimilarQuizSubmissionJpaRepository jpaRepository;

    @Override
    @Transactional
    public SimilarQuizSubmission save(SimilarQuizSubmission submission) {
        SimilarQuizSubmissionJpaEntity entity = SimilarQuizSubmissionJpaEntity.create(
                submission.getSimilarQuizId(), submission.getMemberId(), submission.getScore(),
                submission.getTotalQuestionCount(), submission.getCorrectCount(), submission.getSubmittedAt());

        for (SimilarQuizSubmission.Answer answer : submission.getAnswers()) {
            entity.addAnswer(answer.getQuestionId(), answer.getSelectedIndex(),
                    answer.isCorrect(), answer.getTimeSpentSeconds());
        }

        SimilarQuizSubmissionJpaEntity saved = jpaRepository.save(entity);
        return SimilarQuizSubmission.create(
                saved.getSimilarQuizId(), saved.getMemberId(), saved.getScore(),
                saved.getTotalQuestionCount(), saved.getCorrectCount(), saved.getSubmittedAt(),
                submission.getAnswers());
    }
}
