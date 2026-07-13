package com.wanted.backend.domain.quiz.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_question")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizQuestionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private QuizJpaEntity quiz;

    @Column(name = "question_number", nullable = false)
    private int questionNumber;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    // 난이도 1=하/2=중/3=상 (V3.5.1, nullable). 강사 등록/수정 시 저장.
    @Column(name = "difficulty")
    private Integer difficulty;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    @OrderBy("optionNumber ASC")
    private List<QuizOptionJpaEntity> options = new ArrayList<>();

    static QuizQuestionJpaEntity of(QuizJpaEntity quiz, int questionNumber, String questionText,
                                    String explanation, Integer difficulty) {
        QuizQuestionJpaEntity entity = new QuizQuestionJpaEntity();
        entity.quiz = quiz;
        entity.questionNumber = questionNumber;
        entity.questionText = questionText;
        entity.explanation = explanation;
        entity.difficulty = difficulty;
        return entity;
    }

    void addOption(int optionNumber, String optionText, boolean correct) {
        options.add(QuizOptionJpaEntity.of(this, optionNumber, optionText, correct));
    }
}
