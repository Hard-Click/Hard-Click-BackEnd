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

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
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

    public QuizQuestionJpaEntity addQuestion(int questionNumber, String questionText, String explanation) {
        QuizQuestionJpaEntity question = QuizQuestionJpaEntity.of(this, questionNumber, questionText, explanation);
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
}
