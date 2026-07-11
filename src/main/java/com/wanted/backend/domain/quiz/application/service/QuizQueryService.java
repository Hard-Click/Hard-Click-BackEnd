package com.wanted.backend.domain.quiz.application.service;

import com.wanted.backend.domain.quiz.application.port.CourseSectionTitlePort;
import com.wanted.backend.domain.quiz.application.port.CourseStudentPort;
import com.wanted.backend.domain.quiz.application.port.CourseTitlePort;
import com.wanted.backend.domain.quiz.application.port.EnrollmentAccessPort;
import com.wanted.backend.domain.quiz.application.query.QuizStatisticsQuery;
import com.wanted.backend.domain.quiz.application.result.InstructorQuizDetail;
import com.wanted.backend.domain.quiz.application.result.InstructorQuizStatistics;
import com.wanted.backend.domain.quiz.application.result.InstructorQuizSummary;
import com.wanted.backend.domain.quiz.application.result.MyQuizList;
import com.wanted.backend.domain.quiz.application.result.QuizReport;
import com.wanted.backend.domain.quiz.application.result.StudentQuizDetail;
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

    // 퀴즈 점수는 정답률 백분율(0~100)이므로 만점은 100 고정.
    private static final int MAX_QUIZ_SCORE = 100;

    private final QuizRepository quizRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;
    private final CourseTitlePort courseTitlePort;
    private final CourseSectionTitlePort courseSectionTitlePort;
    private final EnrollmentAccessPort enrollmentAccessPort;
    private final CourseStudentPort courseStudentPort;

    @Override
    public List<InstructorQuizSummary> getInstructorQuizzes(Long instructorId, Long courseId, Long sectionId) {
        List<Quiz> quizzes = quizRepository.findAllByInstructor(instructorId, courseId, sectionId);

        Map<Long, String> courseTitles = courseTitlePort.findTitlesByCourseIds(
                quizzes.stream().map(Quiz::getCourseId).distinct().toList());

        // 섹션명 + 주차(orderIndex)를 함께 조회 — FE가 sectionTitle 파싱 없이 weekNumber를 바로 쓰도록
        Map<Long, CourseSectionTitlePort.SectionInfo> sections = courseSectionTitlePort.findSectionsByIds(
                quizzes.stream().map(Quiz::getSectionId).distinct().toList());

        return quizzes.stream()
                .map(quiz -> new InstructorQuizSummary(
                        quiz.getId(),
                        quiz.getTitle(),
                        quiz.getCourseId(),
                        courseTitles.getOrDefault(quiz.getCourseId(), "강의 #" + quiz.getCourseId()),
                        quiz.getSectionId(),
                        weekOf(sections, quiz.getSectionId()),
                        sectionTitleOf(sections, quiz.getSectionId()),
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
    public StudentQuizDetail getStudentQuizDetail(Long memberId, Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));

        // 수강 중인 강의의 퀴즈만 응시/조회 가능
        if (!enrollmentAccessPort.hasActiveEnrollment(memberId, quiz.getCourseId())) {
            throw new BusinessException(ErrorCode.QUIZ_ENROLLMENT_REQUIRED);
        }

        String courseTitle = courseTitlePort.findTitlesByCourseIds(List.of(quiz.getCourseId()))
                .getOrDefault(quiz.getCourseId(), "강의 #" + quiz.getCourseId());
        String sectionTitle = courseSectionTitlePort.findTitlesBySectionIds(List.of(quiz.getSectionId()))
                .getOrDefault(quiz.getSectionId(), "섹션 #" + quiz.getSectionId());

        List<QuizSubmission> submissions = quizSubmissionRepository
                .findByMemberIdAndQuizIdIn(memberId, List.of(quizId));
        boolean submitted = !submissions.isEmpty();
        int answeredCount = submitted
                ? (int) submissions.get(0).getAnswers().stream()
                        .filter(a -> a.getSelectedOptionId() != null)
                        .count()
                : 0;

        // 정답(correct)/해설(explanation)은 응시 화면에 노출하지 않는다.
        List<StudentQuizDetail.Question> questions = quiz.getQuestions().stream()
                .map(question -> new StudentQuizDetail.Question(
                        question.getId(),
                        question.getQuestionNumber(),
                        question.getQuestionText(),
                        question.getOptions().stream()
                                .map(option -> new StudentQuizDetail.Option(
                                        option.getId(),
                                        option.getOptionNumber(),
                                        option.getOptionText()))
                                .toList()))
                .toList();

        return new StudentQuizDetail(
                quiz.getId(),
                quiz.getTitle(),
                courseTitle,
                sectionTitle,
                questions.size(),
                answeredCount,
                submitted,
                questions);
    }

    @Override
    public QuizReport getMyQuizReport(Long memberId, Long quizId) {
        // 리포트는 학생의 과거 응시 기록이므로, 섹션 삭제로 soft-delete된 퀴즈여도 조회 가능해야 한다.
        Quiz quiz = quizRepository.findByIdIncludingDeleted(quizId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));

        // 수강 중인 강의의 퀴즈 리포트만 조회 가능 (비수강자의 정답/해설 열람 차단)
        if (!enrollmentAccessPort.hasActiveEnrollment(memberId, quiz.getCourseId())) {
            throw new BusinessException(ErrorCode.QUIZ_ENROLLMENT_REQUIRED);
        }

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
        // 이전 주차 점수를 노출해 FE가 "이전 없음(null)"과 "동점(diff=0)"을 구분하게 한다.
        Integer previousScore = previousWeekScore(quiz, courseQuizzes, sections, submissionByQuizId);
        int scoreDiff = previousScore == null ? 0 : submission.getScore() - previousScore;

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
                MAX_QUIZ_SCORE,
                submission.getCorrectCount(),
                submission.getIncorrectCount(),
                scoreDiff,
                previousScore,
                wrongNotes,
                questions);
    }

    @Override
    public InstructorQuizStatistics getInstructorQuizStatistics(QuizStatisticsQuery query) {
        Quiz quiz = quizRepository.findById(query.quizId())
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));

        // 본인이 등록한 퀴즈만 통계 조회 가능
        if (!query.instructorId().equals(quiz.getInstructorId())) {
            throw new BusinessException(ErrorCode.QUIZ_NOT_AUTHORIZED);
        }

        String courseTitle = courseTitlePort.findTitlesByCourseIds(List.of(quiz.getCourseId()))
                .getOrDefault(quiz.getCourseId(), "강의 #" + quiz.getCourseId());
        Map<Long, CourseSectionTitlePort.SectionInfo> sections =
                courseSectionTitlePort.findSectionsByIds(List.of(quiz.getSectionId()));
        String sectionTitle = sectionTitleOf(sections, quiz.getSectionId());
        int week = weekOf(sections, quiz.getSectionId());

        // 전체 수강생 + 각 수강생의 응시(제출) 현황을 조합
        Map<Long, QuizSubmission> submissionByMemberId = quizSubmissionRepository.findByQuizId(query.quizId()).stream()
                .collect(Collectors.toMap(QuizSubmission::getMemberId, Function.identity()));

        List<InstructorQuizStatistics.StudentScore> allStudents = courseStudentPort
                .findActiveStudents(quiz.getCourseId()).stream()
                .map(student -> {
                    QuizSubmission submission = submissionByMemberId.get(student.memberId());
                    return new InstructorQuizStatistics.StudentScore(
                            student.username(),
                            student.name(),
                            submission != null,
                            submission == null ? null : submission.getScore(),
                            submission == null ? null : submission.getSubmittedAt());
                })
                .toList();

        int totalCount = allStudents.size();
        int submittedCount = (int) allStudents.stream()
                .filter(InstructorQuizStatistics.StudentScore::submitted).count();
        int averageScore = (int) Math.round(allStudents.stream()
                .filter(InstructorQuizStatistics.StudentScore::submitted)
                .mapToInt(InstructorQuizStatistics.StudentScore::score)
                .average()
                .orElse(0));

        InstructorQuizStatistics.Summary summary = new InstructorQuizStatistics.Summary(
                totalCount, submittedCount, totalCount - submittedCount, averageScore);

        List<InstructorQuizStatistics.ScoreDistribution> distribution =
                scoreDistribution(allStudents, submittedCount);

        // 수강생 목록: 검색 → 필터 → 정렬 → 페이지네이션
        List<InstructorQuizStatistics.StudentScore> students = allStudents.stream()
                .filter(s -> matchesKeyword(s, query.keyword()))
                .filter(s -> matchesFilter(s, query.filter()))
                .sorted(studentComparator(query.sort()))
                .toList();
        students = paginate(students, query.page(), query.size());

        return new InstructorQuizStatistics(courseTitle, sectionTitle, week, quiz.getTitle(),
                summary, distribution, students);
    }

    private List<InstructorQuizStatistics.ScoreDistribution> scoreDistribution(
            List<InstructorQuizStatistics.StudentScore> students, int submittedCount) {
        int[][] bounds = {{90, 100}, {70, 89}, {50, 69}, {0, 49}};
        String[] labels = {"90~100", "70~89", "50~69", "0~49"};

        List<InstructorQuizStatistics.ScoreDistribution> distribution = new java.util.ArrayList<>();
        for (int i = 0; i < bounds.length; i++) {
            int low = bounds[i][0];
            int high = bounds[i][1];
            int count = (int) students.stream()
                    .filter(InstructorQuizStatistics.StudentScore::submitted)
                    .filter(s -> s.score() >= low && s.score() <= high)
                    .count();
            int percentage = submittedCount == 0 ? 0 : Math.round((float) count * 100 / submittedCount);
            distribution.add(new InstructorQuizStatistics.ScoreDistribution(labels[i], count, percentage));
        }
        return distribution;
    }

    private boolean matchesKeyword(InstructorQuizStatistics.StudentScore s, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String kw = keyword.strip();
        return s.name().contains(kw) || s.userId().contains(kw);
    }

    private boolean matchesFilter(InstructorQuizStatistics.StudentScore s, QuizStatisticsQuery.FilterType filter) {
        return switch (filter) {
            case ALL -> true;
            case SUBMITTED -> s.submitted();
            case NOT_SUBMITTED -> !s.submitted();
        };
    }

    private Comparator<InstructorQuizStatistics.StudentScore> studentComparator(QuizStatisticsQuery.SortType sort) {
        // 동점자/동명 순서 결정성을 위해 userId를 2차 정렬 키로 사용 (페이지네이션 안정성 보장).
        Comparator<InstructorQuizStatistics.StudentScore> primary = switch (sort) {
            // 미응시(score null)는 항상 뒤로
            case SCORE_DESC -> Comparator.comparing(InstructorQuizStatistics.StudentScore::score,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            case SCORE_ASC -> Comparator.comparing(InstructorQuizStatistics.StudentScore::score,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case NAME -> Comparator.comparing(InstructorQuizStatistics.StudentScore::name);
        };
        return primary.thenComparing(InstructorQuizStatistics.StudentScore::userId);
    }

    private <T> List<T> paginate(List<T> items, int page, int size) {
        int fromIndex = page * size;
        if (fromIndex >= items.size()) {
            return List.of();
        }
        return items.subList(fromIndex, Math.min(fromIndex + size, items.size()));
    }

    private int weekOf(Map<Long, CourseSectionTitlePort.SectionInfo> sections, Long sectionId) {
        CourseSectionTitlePort.SectionInfo info = sections.get(sectionId);
        return info == null ? 0 : info.orderIndex();
    }

    private String sectionTitleOf(Map<Long, CourseSectionTitlePort.SectionInfo> sections, Long sectionId) {
        CourseSectionTitlePort.SectionInfo info = sections.get(sectionId);
        return info == null ? "섹션 #" + sectionId : info.title();
    }

    // 바로 이전 주차(내가 제출한) 퀴즈 점수. 이전 주차 제출이 없으면 null (동점 diff=0과 구분 가능).
    private Integer previousWeekScore(Quiz currentQuiz, List<Quiz> courseQuizzes,
                                       Map<Long, CourseSectionTitlePort.SectionInfo> sections,
                                       Map<Long, QuizSubmission> submissionByQuizId) {
        int currentWeek = weekOf(sections, currentQuiz.getSectionId());

        return courseQuizzes.stream()
                .filter(q -> !q.getId().equals(currentQuiz.getId()))
                .filter(q -> submissionByQuizId.containsKey(q.getId()))
                .filter(q -> weekOf(sections, q.getSectionId()) < currentWeek)
                .max(Comparator.comparingInt(q -> weekOf(sections, q.getSectionId())))
                .map(q -> submissionByQuizId.get(q.getId()).getScore())
                .orElse(null);
    }
}
