package com.wanted.backend.domain.quiz.application.service;

import com.wanted.backend.domain.quiz.application.command.CreateQuizCommand;
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
        CourseOwnershipPort.CourseSectionOwnership ownership = courseOwnershipPort
                .findOwnership(command.courseId(), command.sectionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        if (!ownership.instructorId().equals(command.instructorId())) {
            throw new BusinessException(ErrorCode.COURSE_ACCESS_DENIED);
        }
        if (!ownership.sectionBelongsToCourse()) {
            throw new BusinessException(ErrorCode.COURSE_SECTION_NOT_FOUND);
        }

        List<QuizQuestion> questions = IntStream.range(0, command.questions().size())
                .mapToObj(i -> {
                    CreateQuizCommand.QuestionCommand q = command.questions().get(i);
                    return QuizQuestion.create(i + 1, q.questionText(), q.explanation(),
                            q.correctOptionNumber(), q.optionTexts());
                })
                .toList();

        Quiz quiz = Quiz.create(command.instructorId(), command.courseId(), command.sectionId(),
                command.quizTitle(), questions);

        return quizRepository.save(quiz).getId();
    }
}
