package com.wanted.backend.domain.quiz.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "quiz_submission_answer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizSubmissionAnswerJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answer_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private QuizSubmissionJpaEntity submission;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "selected_option_id")
    private Long selectedOptionId;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    static QuizSubmissionAnswerJpaEntity of(QuizSubmissionJpaEntity submission, Long questionId,
                                             Long selectedOptionId, boolean correct) {
        QuizSubmissionAnswerJpaEntity entity = new QuizSubmissionAnswerJpaEntity();
        entity.submission = submission;
        entity.questionId = questionId;
        entity.selectedOptionId = selectedOptionId;
        entity.correct = correct;
        return entity;
    }
}
