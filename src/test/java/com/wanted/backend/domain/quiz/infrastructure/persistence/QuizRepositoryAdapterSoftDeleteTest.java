package com.wanted.backend.domain.quiz.infrastructure.persistence;

import com.wanted.backend.domain.quiz.domain.model.Quiz;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 섹션 삭제 cascade의 핵심 계약을 실제 JPA로 검증한다:
 * 퀴즈는 soft-delete(deleted_at)되고, 학생 제출 이력(quiz_submission)은 보존되며,
 * 활성 조회는 삭제된 퀴즈를 제외하되 리포트용 조회는 여전히 반환한다.
 */
@DataJpaTest(properties = {
        "spring.jpa.database=H2",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@ActiveProfiles("test")
@Import(QuizRepositoryAdapter.class)
class QuizRepositoryAdapterSoftDeleteTest {

    private static final Long COURSE_ID = 100L;
    private static final Long INSTRUCTOR_ID = 1L;
    private static final Long MEMBER_ID = 7L;

    @Autowired
    private QuizRepositoryAdapter adapter;

    @Autowired
    private TestEntityManager em;

    private Long persistQuiz(Long sectionId, String title) {
        QuizJpaEntity quiz = QuizJpaEntity.create(COURSE_ID, sectionId, INSTRUCTOR_ID, title,
                LocalDateTime.of(2026, 5, 10, 0, 0));
        em.persist(quiz);
        return quiz.getId();
    }

    private Long persistSubmission(Long quizId) {
        QuizSubmissionJpaEntity submission = QuizSubmissionJpaEntity.create(
                quizId, MEMBER_ID, 80, 2, 1, LocalDateTime.of(2026, 5, 12, 0, 0));
        em.persist(submission);
        return submission.getId();
    }

    @Test
    @DisplayName("섹션 삭제 cascade는 퀴즈를 soft-delete하고 제출 이력은 보존한다")
    void deleteBySectionIdsSoftDeletesQuizAndPreservesSubmissions() {
        Long quizId = persistQuiz(200L, "2주차 퀴즈");
        Long submissionId = persistSubmission(quizId);
        em.flush();
        em.clear();

        adapter.deleteBySectionIds(List.of(200L));
        em.flush();
        em.clear();

        QuizJpaEntity reloaded = em.find(QuizJpaEntity.class, quizId);
        assertThat(reloaded).isNotNull();                                  // 행은 남는다
        assertThat(reloaded.getDeletedAt()).isNotNull();                   // soft-delete 됨

        assertThat(em.find(QuizSubmissionJpaEntity.class, submissionId)).isNotNull(); // 제출 이력 보존

        assertThat(adapter.findById(quizId)).isEmpty();                    // 활성 단건 조회 제외
        assertThat(adapter.findByIdIncludingDeleted(quizId)).isPresent();  // 리포트용은 조회됨
        assertThat(adapter.findAllByCourseId(COURSE_ID)).isEmpty();        // 목록 제외
        assertThat(adapter.findAllByInstructor(INSTRUCTOR_ID, null, null)).isEmpty();
    }

    @Test
    @DisplayName("강사 수동 삭제(deleteById)도 soft-delete하고 제출 이력은 보존한다")
    void deleteByIdSoftDeletesQuizAndPreservesSubmissions() {
        Long quizId = persistQuiz(200L, "삭제 대상 퀴즈");
        Long submissionId = persistSubmission(quizId);
        em.flush();
        em.clear();

        adapter.deleteById(quizId);
        em.flush();
        em.clear();

        assertThat(em.find(QuizJpaEntity.class, quizId).getDeletedAt()).isNotNull();     // soft-delete 됨
        assertThat(em.find(QuizSubmissionJpaEntity.class, submissionId)).isNotNull();    // 제출 이력 보존
        assertThat(adapter.findById(quizId)).isEmpty();                                  // 활성 조회 제외
        assertThat(adapter.findByIdIncludingDeleted(quizId)).isPresent();                // 리포트용은 조회됨
    }

    @Test
    @DisplayName("삭제 대상 섹션의 퀴즈만 soft-delete되고 다른 섹션은 영향 없다")
    void deleteBySectionIdsOnlyAffectsTargetedSections() {
        Long keptQuizId = persistQuiz(100L, "1주차 퀴즈");
        Long droppedQuizId = persistQuiz(200L, "2주차 퀴즈");
        em.flush();
        em.clear();

        adapter.deleteBySectionIds(List.of(200L));
        em.flush();
        em.clear();

        assertThat(em.find(QuizJpaEntity.class, keptQuizId).getDeletedAt()).isNull();
        assertThat(em.find(QuizJpaEntity.class, droppedQuizId).getDeletedAt()).isNotNull();
        assertThat(adapter.findAllByCourseId(COURSE_ID))
                .extracting(Quiz::getId)
                .containsExactly(keptQuizId);
    }
}
