package com.wanted.backend.domain.quiz.application.service;

import com.wanted.backend.domain.quiz.application.command.CreateQuizCommand;
import com.wanted.backend.domain.quiz.application.command.DeleteQuizCommand;
import com.wanted.backend.domain.quiz.application.command.QuizQuestionCommand;
import com.wanted.backend.domain.quiz.application.command.UpdateQuizCommand;
import com.wanted.backend.domain.quiz.application.port.CourseOwnershipPort;
import com.wanted.backend.domain.quiz.application.usecase.QuizCommandUseCase;
import com.wanted.backend.domain.quiz.domain.model.Quiz;
import com.wanted.backend.domain.quiz.domain.model.QuizQuestion;
import com.wanted.backend.domain.quiz.domain.repository.QuizRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional
public class QuizCommandService implements QuizCommandUseCase {

    private final QuizRepository quizRepository;
    private final CourseOwnershipPort courseOwnershipPort;

    @Override
    public Long create(CreateQuizCommand command) {
        validateCourseOwnership(command.courseId(), command.sectionId(), command.instructorId());

        Quiz quiz = Quiz.create(command.instructorId(), command.courseId(), command.sectionId(),
                command.quizTitle(), toQuestions(command.questions()));

        return quizRepository.save(quiz).getId();
    }

    @Override
    public Long update(UpdateQuizCommand command) {
        Quiz quiz = quizRepository.findById(command.quizId())
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));

        if (!quiz.getInstructorId().equals(command.instructorId())) {
            throw new BusinessException(ErrorCode.QUIZ_NOT_AUTHORIZED);
        }
        validateCourseOwnership(command.courseId(), command.sectionId(), command.instructorId());

        quiz.update(command.courseId(), command.sectionId(), command.quizTitle(),
                toQuestions(command.questions()));

        return quizRepository.update(quiz).getId();
    }

    @Override
    public void delete(DeleteQuizCommand command) {
        Quiz quiz = quizRepository.findById(command.quizId())
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));

        if (!quiz.getInstructorId().equals(command.instructorId())) {
            throw new BusinessException(ErrorCode.QUIZ_NOT_AUTHORIZED);
        }

        quizRepository.deleteById(command.quizId());
    }

    private void validateCourseOwnership(Long courseId, Long sectionId, Long instructorId) {
        CourseOwnershipPort.CourseSectionOwnership ownership = courseOwnershipPort
                .findOwnership(courseId, sectionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        if (!ownership.instructorId().equals(instructorId)) {
            throw new BusinessException(ErrorCode.COURSE_ACCESS_DENIED);
        }
        if (!ownership.sectionBelongsToCourse()) {
            throw new BusinessException(ErrorCode.COURSE_SECTION_NOT_FOUND);
        }
    }

    private List<QuizQuestion> toQuestions(List<QuizQuestionCommand> questionCommands) {
        return IntStream.range(0, questionCommands.size())
                .mapToObj(i -> {
                    QuizQuestionCommand q = questionCommands.get(i);
                    return QuizQuestion.create(i + 1, q.questionText(), q.explanation(),
                            q.correctOptionNumber(), q.optionTexts());
                })
                .toList();
    }
}
