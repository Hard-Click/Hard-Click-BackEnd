package com.wanted.backend.domain.quiz.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "similar_quiz", indexes = {
        @Index(name = "idx_similar_quiz_member_id", columnList = "member_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SimilarQuizJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "similar_quiz_id")
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    // 진입 파라미터(주차)는 캘린더 확정 전까지 선택 값 — NULL 허용.
    @Column(name = "week_number")
    private Integer week;

    @Column(nullable = false)
    private String title;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "similarQuiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    @OrderBy("questionOrder ASC")
    private List<SimilarQuizQuestionJpaEntity> questions = new ArrayList<>();

    public static SimilarQuizJpaEntity create(Long memberId, Long courseId, Integer week, String title,
                                              LocalDateTime createdAt) {
        SimilarQuizJpaEntity entity = new SimilarQuizJpaEntity();
        entity.memberId = memberId;
        entity.courseId = courseId;
        entity.week = week;
        entity.title = title;
        entity.createdAt = createdAt;
        return entity;
    }

    public void addQuestion(Long questionId, int questionOrder) {
        questions.add(SimilarQuizQuestionJpaEntity.of(this, questionId, questionOrder));
    }
}
