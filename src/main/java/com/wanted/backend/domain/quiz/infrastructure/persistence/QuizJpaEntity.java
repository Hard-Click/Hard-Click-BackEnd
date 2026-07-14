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
@Table(name = "quiz", indexes = {
        @Index(name = "idx_quiz_course_section", columnList = "course_id, section_id"),
        @Index(name = "idx_quiz_instructor_id", columnList = "instructor_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_id")
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "section_id", nullable = false)
    private Long sectionId;

    @Column(name = "instructor_id", nullable = false)
    private Long instructorId;

    @Column(nullable = false)
    private String title;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 소프트 삭제 시각. NULL = 활성. 섹션 삭제 cascade가 hard-delete 대신 이 값을 채운다
    // (학생 제출 이력 quiz_submission을 보존하기 위함).
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    @OrderBy("questionNumber ASC")
    private List<QuizQuestionJpaEntity> questions = new ArrayList<>();

    public static QuizJpaEntity create(Long courseId, Long sectionId, Long instructorId, String title,
                                        LocalDateTime createdAt) {
        QuizJpaEntity entity = new QuizJpaEntity();
        entity.courseId = courseId;
        entity.sectionId = sectionId;
        entity.instructorId = instructorId;
        entity.title = title;
        entity.createdAt = createdAt;
        return entity;
    }

    public QuizQuestionJpaEntity addQuestion(int questionNumber, String questionText, String explanation,
                                             Integer difficulty) {
        QuizQuestionJpaEntity question = QuizQuestionJpaEntity.of(this, questionNumber, questionText, explanation, difficulty);
        questions.add(question);
        return question;
    }

    public void update(Long courseId, Long sectionId, String title) {
        this.courseId = courseId;
        this.sectionId = sectionId;
        this.title = title;
    }

    public void clearQuestions() {
        questions.clear();
    }

    // 섹션 삭제 cascade용 소프트 삭제. 행·제출 이력은 남기고 활성 조회에서만 제외된다.
    public void softDelete(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
