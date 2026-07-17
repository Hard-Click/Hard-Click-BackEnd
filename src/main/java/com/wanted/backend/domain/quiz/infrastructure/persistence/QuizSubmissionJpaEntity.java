package com.wanted.backend.domain.quiz.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_submission", indexes = {
        @Index(name = "idx_quiz_submission_member_id", columnList = "member_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_quiz_submission_quiz_member", columnNames = {"quiz_id", "member_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizSubmissionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "submission_id")
    private Long id;

    @Column(name = "quiz_id", nullable = false)
    private Long quizId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private int score;

    @Column(name = "total_question_count", nullable = false)
    private int totalQuestionCount;

    @Column(name = "correct_count", nullable = false)
    private int correctCount;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<QuizSubmissionAnswerJpaEntity> answers = new ArrayList<>();

    public static QuizSubmissionJpaEntity create(Long quizId, Long memberId, int score,
                                                  int totalQuestionCount, int correctCount,
                                                  LocalDateTime submittedAt) {
        QuizSubmissionJpaEntity entity = new QuizSubmissionJpaEntity();
        entity.quizId = quizId;
        entity.memberId = memberId;
        entity.score = score;
        entity.totalQuestionCount = totalQuestionCount;
        entity.correctCount = correctCount;
        entity.submittedAt = submittedAt;
        return entity;
    }

    public void addAnswer(Long questionId, Long selectedOptionId, boolean correct, Integer timeSpentSeconds) {
        answers.add(QuizSubmissionAnswerJpaEntity.of(this, questionId, selectedOptionId, correct, timeSpentSeconds));
    }
}
