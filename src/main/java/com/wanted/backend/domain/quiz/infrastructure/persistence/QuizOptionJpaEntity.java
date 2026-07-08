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
@Table(name = "quiz_option")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizOptionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestionJpaEntity question;

    @Column(name = "option_number", nullable = false)
    private int optionNumber;

    @Column(name = "option_text", nullable = false, columnDefinition = "TEXT")
    private String optionText;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    static QuizOptionJpaEntity of(QuizQuestionJpaEntity question, int optionNumber, String optionText, boolean correct) {
        QuizOptionJpaEntity entity = new QuizOptionJpaEntity();
        entity.question = question;
        entity.optionNumber = optionNumber;
        entity.optionText = optionText;
        entity.correct = correct;
        return entity;
    }
}
