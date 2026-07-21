package com.wanted.backend.domain.quiz.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "similar_quiz_submission_answer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SimilarQuizSubmissionAnswerJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answer_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private SimilarQuizSubmissionJpaEntity submission;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "selected_index")
    private Integer selectedIndex;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds;

    static SimilarQuizSubmissionAnswerJpaEntity of(SimilarQuizSubmissionJpaEntity submission, Long questionId,
                                                   Integer selectedIndex, boolean correct, Integer timeSpentSeconds) {
        SimilarQuizSubmissionAnswerJpaEntity entity = new SimilarQuizSubmissionAnswerJpaEntity();
        entity.submission = submission;
        entity.questionId = questionId;
        entity.selectedIndex = selectedIndex;
        entity.correct = correct;
        entity.timeSpentSeconds = timeSpentSeconds;
        return entity;
    }
}
