package com.wanted.backend.domain.quiz.application.service;

import com.wanted.backend.domain.quiz.application.command.CreateAdminQuizCommand;
import com.wanted.backend.domain.quiz.application.command.CreateQuizCommand;
import com.wanted.backend.domain.quiz.application.command.DeleteQuizCommand;
import com.wanted.backend.domain.quiz.application.command.QuizQuestionCommand;
import com.wanted.backend.domain.quiz.application.command.UpdateAdminQuizCommand;
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
        CourseOwnershipPort.CourseSectionOwnership ownership = requireSection(command.courseId(), command.sectionId());
        if (!ownership.instructorId().equals(command.instructorId())) {
            throw new BusinessException(ErrorCode.COURSE_ACCESS_DENIED);
        }

        return saveQuiz(command.instructorId(), command.courseId(), command.sectionId(),
                command.quizTitle(), command.questions());
    }

    // 관리자(ADMIN)용 — 대상 강의의 주인에게 귀속하여 등록. 인가는 컨트롤러 @PreAuthorize("hasRole('ADMIN')")가 보장한다.
    @Override
    public Long createByAdmin(CreateAdminQuizCommand command) {
        CourseOwnershipPort.CourseSectionOwnership ownership = requireSection(command.courseId(), command.sectionId());

        return saveQuiz(ownership.instructorId(), command.courseId(), command.sectionId(),
                command.quizTitle(), command.questions());
    }

    private Long saveQuiz(Long instructorId, Long courseId, Long sectionId,
                          String quizTitle, List<QuizQuestionCommand> questions) {
        Quiz quiz = Quiz.create(instructorId, courseId, sectionId, quizTitle, toQuestions(questions));
        return quizRepository.save(quiz).getId();
    }

    @Override
    public Long update(UpdateQuizCommand command) {
        Quiz quiz = loadQuiz(command.quizId());

        if (!quiz.getInstructorId().equals(command.instructorId())) {
            throw new BusinessException(ErrorCode.QUIZ_NOT_AUTHORIZED);
        }
        validateCourseOwnership(command.courseId(), command.sectionId(), command.instructorId());

        return applyUpdate(quiz, command.courseId(), command.sectionId(),
                command.quizTitle(), command.questions());
    }

    // 관리자(ADMIN)용 — 소유권 검증 없이 수정. 인가는 컨트롤러 @PreAuthorize("hasRole('ADMIN')")가 보장한다.
    @Override
    public Long updateByAdmin(UpdateAdminQuizCommand command) {
        Quiz quiz = loadQuiz(command.quizId());
        requireSection(command.courseId(), command.sectionId());

        return applyUpdate(quiz, command.courseId(), command.sectionId(),
                command.quizTitle(), command.questions());
    }

    private Quiz loadQuiz(Long quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));
    }

    private Long applyUpdate(Quiz quiz, Long courseId, Long sectionId,
                            String quizTitle, List<QuizQuestionCommand> questions) {
        quiz.update(courseId, sectionId, quizTitle, toQuestions(questions));
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

    // 관리자(ADMIN)용 — 소유권 검증 없이 삭제. 인가는 컨트롤러 @PreAuthorize("hasRole('ADMIN')")가 보장한다.
    @Override
    public void deleteQuiz(Long quizId) {
        if (quizRepository.findById(quizId).isEmpty()) {
            throw new BusinessException(ErrorCode.QUIZ_NOT_FOUND);
        }
        quizRepository.deleteById(quizId);
    }

    @Override
    public void deleteBySectionIds(List<Long> sectionIds) {
        if (sectionIds == null || sectionIds.isEmpty()) {
            return;
        }
        quizRepository.deleteBySectionIds(sectionIds);
    }

    private void validateCourseOwnership(Long courseId, Long sectionId, Long instructorId) {
        CourseOwnershipPort.CourseSectionOwnership ownership = requireSection(courseId, sectionId);
        if (!ownership.instructorId().equals(instructorId)) {
            throw new BusinessException(ErrorCode.COURSE_ACCESS_DENIED);
        }
    }

    // 강의/섹션 존재 + 섹션이 강의 소속인지 검증하고 소유권 정보를 반환한다(소유자 검증은 호출부에서).
    private CourseOwnershipPort.CourseSectionOwnership requireSection(Long courseId, Long sectionId) {
        CourseOwnershipPort.CourseSectionOwnership ownership = courseOwnershipPort
                .findOwnership(courseId, sectionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
        if (!ownership.sectionBelongsToCourse()) {
            throw new BusinessException(ErrorCode.COURSE_SECTION_NOT_FOUND);
        }
        return ownership;
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
