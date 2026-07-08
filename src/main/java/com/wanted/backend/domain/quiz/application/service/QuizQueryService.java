package com.wanted.backend.domain.quiz.application.service;

import com.wanted.backend.domain.quiz.application.port.CourseSectionTitlePort;
import com.wanted.backend.domain.quiz.application.port.CourseTitlePort;
import com.wanted.backend.domain.quiz.application.port.EnrollmentAccessPort;
import com.wanted.backend.domain.quiz.application.result.InstructorQuizDetail;
import com.wanted.backend.domain.quiz.application.result.InstructorQuizSummary;
import com.wanted.backend.domain.quiz.application.result.MyQuizList;
import com.wanted.backend.domain.quiz.application.result.QuizReport;
import com.wanted.backend.domain.quiz.application.usecase.QuizQueryUseCase;
import com.wanted.backend.domain.quiz.domain.model.Quiz;
import com.wanted.backend.domain.quiz.domain.model.QuizOption;
import com.wanted.backend.domain.quiz.domain.model.QuizQuestion;
import com.wanted.backend.domain.quiz.domain.model.QuizSubmission;
import com.wanted.backend.domain.quiz.domain.model.QuizSubmissionAnswer;
import com.wanted.backend.domain.quiz.domain.repository.QuizRepository;
import com.wanted.backend.domain.quiz.domain.repository.QuizSubmissionRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizQueryService implements QuizQueryUseCase {

    private final QuizRepository quizRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;
    private final CourseTitlePort courseTitlePort;
    private final CourseSectionTitlePort courseSectionTitlePort;
    private final EnrollmentAccessPort enrollmentAccessPort;

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

    @Override
    public MyQuizList getMyQuizzes(Long memberId, Long courseId) {
        // 수강 중인 강의의 퀴즈만 조회 가능 (미수강 강의의 퀴즈 구성 노출 차단)
        if (!enrollmentAccessPort.hasActiveEnrollment(memberId, courseId)) {
            throw new BusinessException(ErrorCode.QUIZ_ENROLLMENT_REQUIRED);
        }

        List<Quiz> quizzes = quizRepository.findAllByCourseId(courseId);

        String courseTitle = courseTitlePort.findTitlesByCourseIds(List.of(courseId))
                .getOrDefault(courseId, "강의 #" + courseId);

        // 내 제출 이력을 강의 퀴즈 id 묶음으로 한 번에 조회 (N+1 방지)
        Map<Long, QuizSubmission> submissionByQuizId = quizSubmissionRepository
                .findByMemberIdAndQuizIdIn(memberId, quizzes.stream().map(Quiz::getId).toList()).stream()
                .collect(Collectors.toMap(QuizSubmission::getQuizId, Function.identity()));

        Map<Long, CourseSectionTitlePort.SectionInfo> sections = courseSectionTitlePort
                .findSectionsByIds(quizzes.stream().map(Quiz::getSectionId).distinct().toList());

        List<MyQuizList.MyQuizItem> items = quizzes.stream()
                .map(quiz -> {
                    QuizSubmission submission = submissionByQuizId.get(quiz.getId());
                    CourseSectionTitlePort.SectionInfo section = sections.get(quiz.getSectionId());
                    return new MyQuizList.MyQuizItem(
                            quiz.getId(),
                            section == null ? 0 : section.orderIndex(),
                            quiz.getTitle(),
                            quiz.getQuestions().size(),
                            submission != null,
                            submission == null ? null : submission.getScore(),
                            submission == null ? null : submission.getSubmittedAt());
                })
                .sorted(Comparator.comparingInt(MyQuizList.MyQuizItem::weekNumber)
                        .thenComparing(MyQuizList.MyQuizItem::quizId))
                .toList();

        int completedCount = (int) items.stream().filter(MyQuizList.MyQuizItem::completed).count();
        int averageScore = (int) Math.round(items.stream()
                .filter(MyQuizList.MyQuizItem::completed)
                .mapToInt(MyQuizList.MyQuizItem::score)
                .average()
                .orElse(0));

        return new MyQuizList(courseId, courseTitle, completedCount, averageScore, items);
    }

    @Override
    public QuizReport getMyQuizReport(Long memberId, Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));

        // 같은 강의의 퀴즈/섹션/내 제출을 한 번에 확보 (현재 제출 확인 + scoreDiff 계산 공용)
        List<Quiz> courseQuizzes = quizRepository.findAllByCourseId(quiz.getCourseId());
        Map<Long, CourseSectionTitlePort.SectionInfo> sections = courseSectionTitlePort
                .findSectionsByIds(courseQuizzes.stream().map(Quiz::getSectionId).distinct().toList());
        Map<Long, QuizSubmission> submissionByQuizId = quizSubmissionRepository
                .findByMemberIdAndQuizIdIn(memberId, courseQuizzes.stream().map(Quiz::getId).toList()).stream()
                .collect(Collectors.toMap(QuizSubmission::getQuizId, Function.identity()));

        QuizSubmission submission = submissionByQuizId.get(quizId);
        if (submission == null) {
            throw new BusinessException(ErrorCode.QUIZ_SUBMISSION_NOT_FOUND);
        }

        int week = weekOf(sections, quiz.getSectionId());
        int scoreDiff = calculateScoreDiff(quiz, submission.getScore(), courseQuizzes, sections, submissionByQuizId);

        Map<Long, QuizSubmissionAnswer> answerByQuestionId = submission.getAnswers().stream()
                .collect(Collectors.toMap(QuizSubmissionAnswer::getQuestionId, Function.identity()));

        // 리포트는 제출 후 조회이므로 정답/해설을 노출한다. 문항/보기는 번호순 정렬.
        List<QuizReport.QuestionResult> questions = quiz.getQuestions().stream()
                .sorted(Comparator.comparingInt(QuizQuestion::getQuestionNumber))
                .map(question -> {
                    QuizSubmissionAnswer answer = answerByQuestionId.get(question.getId());
                    Long correctOptionId = question.getOptions().stream()
                            .filter(QuizOption::isCorrect)
                            .map(QuizOption::getId)
                            .findFirst()
                            .orElse(null);
                    return new QuizReport.QuestionResult(
                            question.getId(),
                            question.getQuestionNumber(),
                            question.getQuestionText(),
                            correctOptionId,
                            answer == null ? null : answer.getSelectedOptionId(),
                            answer != null && answer.isCorrect(),
                            question.getExplanation(),
                            question.getOptions().stream()
                                    .sorted(Comparator.comparingInt(QuizOption::getOptionNumber))
                                    .map(option -> new QuizReport.OptionView(
                                            option.getId(), option.getOptionNumber(), option.getOptionText()))
                                    .toList());
                })
                .toList();

        List<QuizReport.QuestionResult> wrongNotes = questions.stream()
                .filter(q -> !q.correct())
                .toList();

        return new QuizReport(
                quiz.getId(),
                week,
                quiz.getTitle(),
                submission.getSubmittedAt(),
                submission.getScore(),
                100,
                submission.getCorrectCount(),
                submission.getIncorrectCount(),
                scoreDiff,
                wrongNotes,
                questions);
    }

    private int weekOf(Map<Long, CourseSectionTitlePort.SectionInfo> sections, Long sectionId) {
        CourseSectionTitlePort.SectionInfo info = sections.get(sectionId);
        return info == null ? 0 : info.orderIndex();
    }

    // scoreDiff = 현재 점수 − 바로 이전 주차(내가 제출한) 퀴즈 점수. 이전 주차 제출이 없으면 0.
    private int calculateScoreDiff(Quiz currentQuiz, int currentScore, List<Quiz> courseQuizzes,
                                    Map<Long, CourseSectionTitlePort.SectionInfo> sections,
                                    Map<Long, QuizSubmission> submissionByQuizId) {
        int currentWeek = weekOf(sections, currentQuiz.getSectionId());

        Integer previousScore = courseQuizzes.stream()
                .filter(q -> !q.getId().equals(currentQuiz.getId()))
                .filter(q -> submissionByQuizId.containsKey(q.getId()))
                .filter(q -> weekOf(sections, q.getSectionId()) < currentWeek)
                .max(Comparator.comparingInt(q -> weekOf(sections, q.getSectionId())))
                .map(q -> submissionByQuizId.get(q.getId()).getScore())
                .orElse(null);

        return previousScore == null ? 0 : currentScore - previousScore;
    }
}
