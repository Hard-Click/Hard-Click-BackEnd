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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "similar_quiz_submission", indexes = {
        @Index(name = "idx_sqs_member_id", columnList = "member_id"),
        @Index(name = "idx_sqs_similar_quiz_id", columnList = "similar_quiz_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SimilarQuizSubmissionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "submission_id")
    private Long id;

    @Column(name = "similar_quiz_id", nullable = false)
    private Long similarQuizId;

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
    private List<SimilarQuizSubmissionAnswerJpaEntity> answers = new ArrayList<>();

    public static SimilarQuizSubmissionJpaEntity create(Long similarQuizId, Long memberId, int score,
                                                        int totalQuestionCount, int correctCount,
                                                        LocalDateTime submittedAt) {
        SimilarQuizSubmissionJpaEntity entity = new SimilarQuizSubmissionJpaEntity();
        entity.similarQuizId = similarQuizId;
        entity.memberId = memberId;
        entity.score = score;
        entity.totalQuestionCount = totalQuestionCount;
        entity.correctCount = correctCount;
        entity.submittedAt = submittedAt;
        return entity;
    }

    public void addAnswer(Long questionId, Integer selectedIndex, boolean correct, Integer timeSpentSeconds) {
        answers.add(SimilarQuizSubmissionAnswerJpaEntity.of(this, questionId, selectedIndex, correct, timeSpentSeconds));
    }
}
