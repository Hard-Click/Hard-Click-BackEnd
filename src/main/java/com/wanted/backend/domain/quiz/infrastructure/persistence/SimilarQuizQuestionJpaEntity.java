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
@Table(name = "similar_quiz_question")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SimilarQuizQuestionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "similar_quiz_id", nullable = false)
    private SimilarQuizJpaEntity similarQuiz;

    // 기존 quiz_question 행을 참조한다(정답/해설은 원문항에서 조회).
    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "question_order", nullable = false)
    private int questionOrder;

    static SimilarQuizQuestionJpaEntity of(SimilarQuizJpaEntity similarQuiz, Long questionId, int questionOrder) {
        SimilarQuizQuestionJpaEntity entity = new SimilarQuizQuestionJpaEntity();
        entity.similarQuiz = similarQuiz;
        entity.questionId = questionId;
        entity.questionOrder = questionOrder;
        return entity;
    }
}
