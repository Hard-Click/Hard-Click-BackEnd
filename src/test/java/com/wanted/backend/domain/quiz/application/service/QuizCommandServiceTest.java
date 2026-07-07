package com.wanted.backend.domain.quiz.application.service;

import com.wanted.backend.domain.quiz.application.command.CreateQuizCommand;
import com.wanted.backend.domain.quiz.application.command.QuizQuestionCommand;
import com.wanted.backend.domain.quiz.application.command.UpdateQuizCommand;
import com.wanted.backend.domain.quiz.application.port.CourseOwnershipPort;
import com.wanted.backend.domain.quiz.domain.model.Quiz;
import com.wanted.backend.domain.quiz.domain.model.QuizQuestion;
import com.wanted.backend.domain.quiz.domain.repository.QuizRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuizCommandServiceTest {

    private static final Long INSTRUCTOR_ID = 1L;
    private static final Long COURSE_ID = 10L;
    private static final Long SECTION_ID = 100L;
    private static final Long QUIZ_ID = 999L;

    private QuizRepository quizRepository;
    private CourseOwnershipPort courseOwnershipPort;
    private QuizCommandService service;

    @BeforeEach
    void setUp() {
        quizRepository = mock(QuizRepository.class);
        courseOwnershipPort = mock(CourseOwnershipPort.class);
        service = new QuizCommandService(quizRepository, courseOwnershipPort);
    }

    private QuizQuestionCommand question() {
        return new QuizQuestionCommand(
                "React의 가상 DOM이란 무엇인가요?", "설명", 2,
                List.of("보기1", "보기2", "보기3", "보기4"));
    }

    private Quiz existingQuiz() {
        QuizQuestion question = QuizQuestion.create(1, "기존 문제", "기존 설명", 1,
                List.of("보기1", "보기2", "보기3", "보기4"));
        return Quiz.restore(QUIZ_ID, INSTRUCTOR_ID, COURSE_ID, SECTION_ID, "기존 퀴즈 제목",
                List.of(question), LocalDateTime.of(2026, 5, 10, 15, 30));
    }

    private void givenOwnedCourse() {
        when(courseOwnershipPort.findOwnership(COURSE_ID, SECTION_ID))
                .thenReturn(Optional.of(new CourseOwnershipPort.CourseSectionOwnership(INSTRUCTOR_ID, true)));
    }

    @Test
    void createSavesAQuizWhenTheCourseAndSectionBelongToTheInstructor() {
        givenOwnedCourse();
        when(quizRepository.save(any())).thenAnswer(invocation -> {
            Quiz quiz = invocation.getArgument(0);
            return Quiz.restore(QUIZ_ID, quiz.getInstructorId(), quiz.getCourseId(), quiz.getSectionId(),
                    quiz.getTitle(), quiz.getQuestions(), quiz.getCreatedAt());
        });

        CreateQuizCommand command = new CreateQuizCommand(
                INSTRUCTOR_ID, COURSE_ID, SECTION_ID, "React 기초 개념 퀴즈", List.of(question()));

        Long quizId = service.create(command);

        assertThat(quizId).isEqualTo(QUIZ_ID);
        ArgumentCaptor<Quiz> captor = ArgumentCaptor.forClass(Quiz.class);
        verify(quizRepository).save(captor.capture());
        assertThat(captor.getValue().getCourseId()).isEqualTo(COURSE_ID);
        assertThat(captor.getValue().getSectionId()).isEqualTo(SECTION_ID);
        assertThat(captor.getValue().getInstructorId()).isEqualTo(INSTRUCTOR_ID);
        assertThat(captor.getValue().getTitle()).isEqualTo("React 기초 개념 퀴즈");
        assertThat(captor.getValue().getQuestions()).hasSize(1);
    }

    @Test
    void createRejectsWhenTheCourseDoesNotExist() {
        when(courseOwnershipPort.findOwnership(COURSE_ID, SECTION_ID)).thenReturn(Optional.empty());

        CreateQuizCommand command = new CreateQuizCommand(
                INSTRUCTOR_ID, COURSE_ID, SECTION_ID, "제목", List.of(question()));

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COURSE_NOT_FOUND);
    }

    @Test
    void createRejectsWhenTheRequesterIsNotTheCourseOwner() {
        when(courseOwnershipPort.findOwnership(COURSE_ID, SECTION_ID))
                .thenReturn(Optional.of(new CourseOwnershipPort.CourseSectionOwnership(999L, true)));

        CreateQuizCommand command = new CreateQuizCommand(
                INSTRUCTOR_ID, COURSE_ID, SECTION_ID, "제목", List.of(question()));

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COURSE_ACCESS_DENIED);
    }

    @Test
    void createRejectsWhenTheSectionDoesNotBelongToTheCourse() {
        when(courseOwnershipPort.findOwnership(COURSE_ID, SECTION_ID))
                .thenReturn(Optional.of(new CourseOwnershipPort.CourseSectionOwnership(INSTRUCTOR_ID, false)));

        CreateQuizCommand command = new CreateQuizCommand(
                INSTRUCTOR_ID, COURSE_ID, SECTION_ID, "제목", List.of(question()));

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COURSE_SECTION_NOT_FOUND);
    }

    @Test
    void updateReplacesTheTitleAndQuestionsWhenTheQuizBelongsToTheInstructor() {
        givenOwnedCourse();
        when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(existingQuiz()));
        when(quizRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateQuizCommand command = new UpdateQuizCommand(
                QUIZ_ID, INSTRUCTOR_ID, COURSE_ID, SECTION_ID, "수정된 퀴즈 제목",
                List.of(question(), question()));

        Long quizId = service.update(command);

        assertThat(quizId).isEqualTo(QUIZ_ID);
        ArgumentCaptor<Quiz> captor = ArgumentCaptor.forClass(Quiz.class);
        verify(quizRepository).update(captor.capture());
        Quiz updated = captor.getValue();
        assertThat(updated.getId()).isEqualTo(QUIZ_ID);
        assertThat(updated.getTitle()).isEqualTo("수정된 퀴즈 제목");
        assertThat(updated.getQuestions()).hasSize(2);
        assertThat(updated.getQuestions().get(0).getQuestionText()).isEqualTo("React의 가상 DOM이란 무엇인가요?");
        assertThat(updated.getQuestions().get(1).getQuestionNumber()).isEqualTo(2);
    }

    @Test
    void updateRejectsWhenTheQuizDoesNotExist() {
        when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.empty());

        UpdateQuizCommand command = new UpdateQuizCommand(
                QUIZ_ID, INSTRUCTOR_ID, COURSE_ID, SECTION_ID, "제목", List.of(question()));

        assertThatThrownBy(() -> service.update(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUIZ_NOT_FOUND);
        verify(quizRepository, never()).update(any());
    }

    @Test
    void updateRejectsWhenTheQuizWasRegisteredByAnotherInstructor() {
        when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(existingQuiz()));

        UpdateQuizCommand command = new UpdateQuizCommand(
                QUIZ_ID, 999L, COURSE_ID, SECTION_ID, "제목", List.of(question()));

        assertThatThrownBy(() -> service.update(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUIZ_NOT_AUTHORIZED);
        verify(quizRepository, never()).update(any());
    }

    @Test
    void updateRejectsWhenTheTargetCourseDoesNotExist() {
        when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(existingQuiz()));
        when(courseOwnershipPort.findOwnership(COURSE_ID, SECTION_ID)).thenReturn(Optional.empty());

        UpdateQuizCommand command = new UpdateQuizCommand(
                QUIZ_ID, INSTRUCTOR_ID, COURSE_ID, SECTION_ID, "제목", List.of(question()));

        assertThatThrownBy(() -> service.update(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COURSE_NOT_FOUND);
    }

    @Test
    void updateRejectsWhenTheTargetCourseBelongsToAnotherInstructor() {
        when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(existingQuiz()));
        when(courseOwnershipPort.findOwnership(COURSE_ID, SECTION_ID))
                .thenReturn(Optional.of(new CourseOwnershipPort.CourseSectionOwnership(999L, true)));

        UpdateQuizCommand command = new UpdateQuizCommand(
                QUIZ_ID, INSTRUCTOR_ID, COURSE_ID, SECTION_ID, "제목", List.of(question()));

        assertThatThrownBy(() -> service.update(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COURSE_ACCESS_DENIED);
    }

    @Test
    void updateRejectsWhenTheTargetSectionDoesNotBelongToTheCourse() {
        when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(existingQuiz()));
        when(courseOwnershipPort.findOwnership(COURSE_ID, SECTION_ID))
                .thenReturn(Optional.of(new CourseOwnershipPort.CourseSectionOwnership(INSTRUCTOR_ID, false)));

        UpdateQuizCommand command = new UpdateQuizCommand(
                QUIZ_ID, INSTRUCTOR_ID, COURSE_ID, SECTION_ID, "제목", List.of(question()));

        assertThatThrownBy(() -> service.update(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COURSE_SECTION_NOT_FOUND);
    }
}
