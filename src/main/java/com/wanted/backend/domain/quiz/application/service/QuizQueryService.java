package com.wanted.backend.domain.quiz.application.service;

import com.wanted.backend.domain.quiz.application.port.CourseSectionTitlePort;
import com.wanted.backend.domain.quiz.application.port.CourseTitlePort;
import com.wanted.backend.domain.quiz.application.result.InstructorQuizDetail;
import com.wanted.backend.domain.quiz.application.result.InstructorQuizSummary;
import com.wanted.backend.domain.quiz.application.usecase.QuizQueryUseCase;
import com.wanted.backend.domain.quiz.domain.model.Quiz;
import com.wanted.backend.domain.quiz.domain.model.QuizOption;
import com.wanted.backend.domain.quiz.domain.repository.QuizRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizQueryService implements QuizQueryUseCase {

    private final QuizRepository quizRepository;
    private final CourseTitlePort courseTitlePort;
    private final CourseSectionTitlePort courseSectionTitlePort;

    @Override
    public List<InstructorQuizSummary> getInstructorQuizzes(Long instructorId, Long courseId, Long sectionId) {
        List<Quiz> quizzes = quizRepository.findAllByInstructor(instructorId, courseId, sectionId);

        Map<Long, String> courseTitles = courseTitlePort.findTitlesByCourseIds(
                quizzes.stream().map(Quiz::getCourseId).distinct().toList());

        Map<Long, String> sectionTitles = courseSectionTitlePort.findTitlesBySectionIds(
                quizzes.stream().map(Quiz::getSectionId).distinct().toList());

        return quizzes.stream()
                .map(quiz -> new InstructorQuizSummary(
                        quiz.getId(),
                        quiz.getTitle(),
                        quiz.getCourseId(),
                        courseTitles.getOrDefault(quiz.getCourseId(), "강의 #" + quiz.getCourseId()),
                        quiz.getSectionId(),
                        sectionTitles.getOrDefault(quiz.getSectionId(), "섹션 #" + quiz.getSectionId()),
                        quiz.getQuestions().size(),
                        quiz.getCreatedAt()))
                .toList();
    }

    @Override
    public InstructorQuizDetail getInstructorQuizDetail(Long instructorId, Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));

        // 인증에서 온 instructorId는 non-null 보장 → equals 좌변에 두어 null-safe 비교
        if (!instructorId.equals(quiz.getInstructorId())) {
            throw new BusinessException(ErrorCode.QUIZ_NOT_AUTHORIZED);
        }

        String courseTitle = courseTitlePort.findTitlesByCourseIds(List.of(quiz.getCourseId()))
                .getOrDefault(quiz.getCourseId(), "강의 #" + quiz.getCourseId());
        String sectionTitle = courseSectionTitlePort.findTitlesBySectionIds(List.of(quiz.getSectionId()))
                .getOrDefault(quiz.getSectionId(), "섹션 #" + quiz.getSectionId());

        List<InstructorQuizDetail.QuestionDetail> questions = quiz.getQuestions().stream()
                .map(question -> new InstructorQuizDetail.QuestionDetail(
                        question.getId(),
                        question.getQuestionNumber(),
                        question.getQuestionText(),
                        question.getOptions().stream()
                                .filter(QuizOption::isCorrect)
                                .map(QuizOption::getId)
                                .findFirst()
                                .orElse(null),
                        question.getExplanation(),
                        question.getOptions().stream()
                                .map(option -> new InstructorQuizDetail.OptionDetail(
                                        option.getId(),
                                        option.getOptionNumber(),
                                        option.getOptionText(),
                                        option.isCorrect()))
                                .toList()))
                .toList();

        return new InstructorQuizDetail(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getCourseId(),
                courseTitle,
                quiz.getSectionId(),
                sectionTitle,
                questions.size(),
                quiz.getCreatedAt(),
                questions);
    }
}
