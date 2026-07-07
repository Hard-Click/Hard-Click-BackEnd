package com.wanted.backend.domain.quiz.application.service;

import com.wanted.backend.domain.quiz.application.command.CreateQuizCommand;
import com.wanted.backend.domain.quiz.application.port.CourseOwnershipPort;
import com.wanted.backend.domain.quiz.domain.model.Quiz;
import com.wanted.backend.domain.quiz.domain.repository.QuizRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuizCommandServiceTest {

    private static final Long INSTRUCTOR_ID = 1L;
    private static final Long COURSE_ID = 10L;
    private static final Long SECTION_ID = 100L;

    private QuizRepository quizRepository;
    private CourseOwnershipPort courseOwnershipPort;
    private QuizCommandService service;

    @BeforeEach
    void setUp() {
        quizRepository = mock(QuizRepository.class);
        courseOwnershipPort = mock(CourseOwnershipPort.class);
        service = new QuizCommandService(quizRepository, courseOwnershipPort);
    }

    private CreateQuizCommand.QuestionCommand question() {
        return new CreateQuizCommand.QuestionCommand(
                "React의 가상 DOM이란 무엇인가요?", "설명", 2,
                List.of("보기1", "보기2", "보기3", "보기4"));
    }

    @Test
    void createSavesAQuizWhenTheCourseAndSectionBelongToTheInstructor() {
        when(courseOwnershipPort.findOwnership(COURSE_ID, SECTION_ID))
                .thenReturn(Optional.of(new CourseOwnershipPort.CourseSectionOwnership(INSTRUCTOR_ID, true)));
        when(quizRepository.save(any())).thenAnswer(invocation -> {
            Quiz quiz = invocation.getArgument(0);
            return Quiz.restore(999L, quiz.getInstructorId(), quiz.getCourseId(), quiz.getSectionId(),
                    quiz.getTitle(), quiz.getQuestions(), quiz.getCreatedAt());
        });

        CreateQuizCommand command = new CreateQuizCommand(
                INSTRUCTOR_ID, COURSE_ID, SECTION_ID, "React 기초 개념 퀴즈", List.of(question()));

        Long quizId = service.create(command);

        assertThat(quizId).isEqualTo(999L);
        ArgumentCaptor<Quiz> captor = ArgumentCaptor.forClass(Quiz.class);
        verify(quizRepository).save(captor.capture());
        assertThat(captor.getValue().getCourseId()).isEqualTo(COURSE_ID);
        assertThat(captor.getValue().getSectionId()).isEqualTo(SECTION_ID);
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
}
