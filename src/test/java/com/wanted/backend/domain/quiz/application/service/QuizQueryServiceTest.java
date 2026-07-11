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
import com.wanted.backend.domain.quiz.domain.model.Quiz;
import com.wanted.backend.domain.quiz.domain.model.QuizOption;
import com.wanted.backend.domain.quiz.domain.model.QuizQuestion;
import com.wanted.backend.domain.quiz.domain.model.QuizSubmission;
import com.wanted.backend.domain.quiz.domain.model.QuizSubmissionAnswer;
import com.wanted.backend.domain.quiz.domain.repository.QuizRepository;
import com.wanted.backend.domain.quiz.domain.repository.QuizSubmissionRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuizQueryServiceTest {

    private static final Long INSTRUCTOR_ID = 1L;
    private static final Long COURSE_ID = 10L;
    private static final Long SECTION_ID = 100L;
    private static final Long MEMBER_ID = 7L;

    private QuizRepository quizRepository;
    private QuizSubmissionRepository quizSubmissionRepository;
    private CourseTitlePort courseTitlePort;
    private CourseSectionTitlePort courseSectionTitlePort;
    private EnrollmentAccessPort enrollmentAccessPort;
    private CourseStudentPort courseStudentPort;
    private QuizQueryService service;

    @BeforeEach
    void setUp() {
        quizRepository = mock(QuizRepository.class);
        quizSubmissionRepository = mock(QuizSubmissionRepository.class);
        courseTitlePort = mock(CourseTitlePort.class);
        courseSectionTitlePort = mock(CourseSectionTitlePort.class);
        enrollmentAccessPort = mock(EnrollmentAccessPort.class);
        courseStudentPort = mock(CourseStudentPort.class);
        service = new QuizQueryService(quizRepository, quizSubmissionRepository,
                courseTitlePort, courseSectionTitlePort, enrollmentAccessPort, courseStudentPort);
    }

    private Quiz quiz(Long id, Long courseId, Long sectionId, String title, int questionCount) {
        List<QuizQuestion> questions = new java.util.ArrayList<>();
        for (int i = 1; i <= questionCount; i++) {
            questions.add(QuizQuestion.create(i, "질문" + i, null, 1,
                    List.of("보기1", "보기2", "보기3", "보기4")));
        }
        return Quiz.restore(id, INSTRUCTOR_ID, courseId, sectionId, title, questions,
                LocalDateTime.of(2026, 5, 10, 15, 30));
    }

    @Test
    void instructorQuizzesIncludeCourseAndSectionTitlesAndQuestionCounts() {
        when(quizRepository.findAllByInstructor(INSTRUCTOR_ID, COURSE_ID, null)).thenReturn(List.of(
                quiz(90L, COURSE_ID, SECTION_ID, "1주차 퀴즈", 8),
                quiz(91L, COURSE_ID, 101L, "2주차 퀴즈", 5)
        ));
        when(courseTitlePort.findTitlesByCourseIds(anyCollection()))
                .thenReturn(Map.of(COURSE_ID, "React 완벽 가이드"));
        when(courseSectionTitlePort.findSectionsByIds(anyCollection())).thenReturn(Map.of(
                SECTION_ID, new CourseSectionTitlePort.SectionInfo("섹션 1: React 기초", 1),
                101L, new CourseSectionTitlePort.SectionInfo("섹션 2: Hooks", 2)));

        List<InstructorQuizSummary> summaries = service.getInstructorQuizzes(INSTRUCTOR_ID, COURSE_ID, null);

        assertThat(summaries).hasSize(2);
        InstructorQuizSummary first = summaries.get(0);
        assertThat(first.quizId()).isEqualTo(90L);
        assertThat(first.quizTitle()).isEqualTo("1주차 퀴즈");
        assertThat(first.courseTitle()).isEqualTo("React 완벽 가이드");
        assertThat(first.sectionId()).isEqualTo(SECTION_ID);
        assertThat(first.weekNumber()).isEqualTo(1);
        assertThat(first.sectionTitle()).isEqualTo("섹션 1: React 기초");
        assertThat(first.questionCount()).isEqualTo(8);
        assertThat(summaries.get(1).weekNumber()).isEqualTo(2);
        assertThat(summaries.get(1).sectionTitle()).isEqualTo("섹션 2: Hooks");
        assertThat(summaries.get(1).questionCount()).isEqualTo(5);
    }

    @Test
    void instructorQuizzesFallBackToPlaceholderTitlesWhenReferencesAreMissing() {
        when(quizRepository.findAllByInstructor(INSTRUCTOR_ID, null, null))
                .thenReturn(List.of(quiz(90L, 999L, 888L, "퀴즈", 1)));
        when(courseTitlePort.findTitlesByCourseIds(anyCollection())).thenReturn(Map.of());
        when(courseSectionTitlePort.findSectionsByIds(anyCollection())).thenReturn(Map.of());

        List<InstructorQuizSummary> summaries = service.getInstructorQuizzes(INSTRUCTOR_ID, null, null);

        assertThat(summaries.get(0).courseTitle()).isEqualTo("강의 #999");
        // dangling 섹션: weekNumber 0 + 섹션명 폴백 → FE 정규식 파싱 실패 대신 명시적 0 제공
        assertThat(summaries.get(0).weekNumber()).isZero();
        assertThat(summaries.get(0).sectionTitle()).isEqualTo("섹션 #888");
    }

    @Test
    void instructorQuizzesReturnAnEmptyListWhenTheInstructorHasNoQuizzes() {
        when(quizRepository.findAllByInstructor(INSTRUCTOR_ID, null, null)).thenReturn(List.of());
        when(courseSectionTitlePort.findSectionsByIds(anyCollection())).thenReturn(Map.of());

        assertThat(service.getInstructorQuizzes(INSTRUCTOR_ID, null, null)).isEmpty();
    }

    @Test
    void instructorQuizDetailReturnsQuestionsWithCorrectOptionResolvedFromTheDomain() {
        // 실제 조회 시나리오처럼 DB에서 복원된(restore) 엔티티 — 문항/보기 id가 채워진 상태
        QuizQuestion question = QuizQuestion.restore(11L, 1, "React의 가상 DOM이란?", "가상 DOM 설명",
                List.of(
                        QuizOption.restore(21L, 1, "보기1", false),
                        QuizOption.restore(22L, 2, "보기2", true),
                        QuizOption.restore(23L, 3, "보기3", false),
                        QuizOption.restore(24L, 4, "보기4", false)));
        Quiz quiz = Quiz.restore(90L, INSTRUCTOR_ID, COURSE_ID, SECTION_ID, "React 기초 개념 퀴즈",
                List.of(question), LocalDateTime.of(2026, 5, 10, 15, 30));
        when(quizRepository.findById(90L)).thenReturn(Optional.of(quiz));
        when(courseTitlePort.findTitlesByCourseIds(anyCollection()))
                .thenReturn(Map.of(COURSE_ID, "React 완벽 가이드"));
        when(courseSectionTitlePort.findTitlesBySectionIds(anyCollection()))
                .thenReturn(Map.of(SECTION_ID, "섹션 1: React 기초"));

        InstructorQuizDetail detail = service.getInstructorQuizDetail(INSTRUCTOR_ID, 90L);

        assertThat(detail.quizId()).isEqualTo(90L);
        assertThat(detail.courseTitle()).isEqualTo("React 완벽 가이드");
        assertThat(detail.sectionTitle()).isEqualTo("섹션 1: React 기초");
        assertThat(detail.questionCount()).isEqualTo(1);
        InstructorQuizDetail.QuestionDetail questionDetail = detail.questions().get(0);
        assertThat(questionDetail.questionId()).isEqualTo(11L);
        assertThat(questionDetail.explanation()).isEqualTo("가상 DOM 설명");
        assertThat(questionDetail.options()).hasSize(4);
        // 두 번째 보기(id 22)가 정답 → correctOptionId가 그 id와 일치
        assertThat(questionDetail.correctOptionId()).isEqualTo(22L);
        assertThat(questionDetail.options().get(1).correct()).isTrue();
    }

    @Test
    void instructorQuizDetailFallsBackToPlaceholderTitlesWhenReferencesAreMissing() {
        Quiz quiz = Quiz.restore(90L, INSTRUCTOR_ID, 999L, 888L, "퀴즈",
                List.of(QuizQuestion.restore(1L, 1, "질문", null,
                        List.of(
                                QuizOption.restore(1L, 1, "보기1", true),
                                QuizOption.restore(2L, 2, "보기2", false),
                                QuizOption.restore(3L, 3, "보기3", false),
                                QuizOption.restore(4L, 4, "보기4", false)))),
                LocalDateTime.of(2026, 5, 10, 15, 30));
        when(quizRepository.findById(90L)).thenReturn(Optional.of(quiz));
        when(courseTitlePort.findTitlesByCourseIds(anyCollection())).thenReturn(Map.of());
        when(courseSectionTitlePort.findTitlesBySectionIds(anyCollection())).thenReturn(Map.of());

        InstructorQuizDetail detail = service.getInstructorQuizDetail(INSTRUCTOR_ID, 90L);

        assertThat(detail.courseTitle()).isEqualTo("강의 #999");
        assertThat(detail.sectionTitle()).isEqualTo("섹션 #888");
    }

    @Test
    void instructorQuizDetailRejectsWhenTheQuizDoesNotExist() {
        when(quizRepository.findById(90L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getInstructorQuizDetail(INSTRUCTOR_ID, 90L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUIZ_NOT_FOUND);
    }

    @Test
    void instructorQuizDetailRejectsWhenTheQuizBelongsToAnotherInstructor() {
        Quiz quiz = quiz(90L, COURSE_ID, SECTION_ID, "퀴즈", 1);
        when(quizRepository.findById(90L)).thenReturn(Optional.of(quiz));

        assertThatThrownBy(() -> service.getInstructorQuizDetail(999L, 90L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUIZ_NOT_AUTHORIZED);
    }

    @Test
    void myQuizzesJoinSubmissionStatusSortByWeekAndSummarizeCompletion() {
        // 강의에 3주차/1주차 퀴즈(정렬 뒤섞임). 1주만 제출(80점), 3주 미제출.
        Quiz week3 = quiz(93L, COURSE_ID, 300L, "State와 Lifecycle", 10);
        Quiz week1 = quiz(90L, COURSE_ID, 100L, "React 기초 개념", 10);
        when(enrollmentAccessPort.hasActiveEnrollment(MEMBER_ID, COURSE_ID)).thenReturn(true);
        when(quizRepository.findAllByCourseId(COURSE_ID)).thenReturn(List.of(week3, week1));
        when(courseTitlePort.findTitlesByCourseIds(anyCollection()))
                .thenReturn(Map.of(COURSE_ID, "React 완벽 가이드"));
        when(courseSectionTitlePort.findSectionsByIds(anyCollection())).thenReturn(Map.of(
                100L, new CourseSectionTitlePort.SectionInfo("섹션 1", 1),
                300L, new CourseSectionTitlePort.SectionInfo("섹션 3", 3)));
        QuizSubmission week1Submission = QuizSubmission.restore(55L, 90L, MEMBER_ID, 80, 10, 8,
                LocalDateTime.of(2026, 5, 12, 0, 0), List.of());
        when(quizSubmissionRepository.findByMemberIdAndQuizIdIn(eq(MEMBER_ID), anyList()))
                .thenReturn(List.of(week1Submission));

        MyQuizList result = service.getMyQuizzes(MEMBER_ID, COURSE_ID);

        assertThat(result.courseId()).isEqualTo(COURSE_ID);
        assertThat(result.courseTitle()).isEqualTo("React 완벽 가이드");
        assertThat(result.completedCount()).isEqualTo(1);
        assertThat(result.averageScore()).isEqualTo(80);
        // 주차 오름차순 정렬 확인 (1주 먼저)
        assertThat(result.quizzes().get(0).weekNumber()).isEqualTo(1);
        assertThat(result.quizzes().get(0).completed()).isTrue();
        assertThat(result.quizzes().get(0).score()).isEqualTo(80);
        assertThat(result.quizzes().get(1).weekNumber()).isEqualTo(3);
        assertThat(result.quizzes().get(1).completed()).isFalse();
        assertThat(result.quizzes().get(1).score()).isNull();
        assertThat(result.quizzes().get(1).submittedAt()).isNull();
    }

    @Test
    void myQuizzesReturnZeroSummaryWhenNothingSubmitted() {
        Quiz week1 = quiz(90L, COURSE_ID, 100L, "React 기초 개념", 10);
        when(enrollmentAccessPort.hasActiveEnrollment(MEMBER_ID, COURSE_ID)).thenReturn(true);
        when(quizRepository.findAllByCourseId(COURSE_ID)).thenReturn(List.of(week1));
        when(courseTitlePort.findTitlesByCourseIds(anyCollection()))
                .thenReturn(Map.of(COURSE_ID, "React 완벽 가이드"));
        when(courseSectionTitlePort.findSectionsByIds(anyCollection()))
                .thenReturn(Map.of(100L, new CourseSectionTitlePort.SectionInfo("섹션 1", 1)));
        when(quizSubmissionRepository.findByMemberIdAndQuizIdIn(eq(MEMBER_ID), anyList()))
                .thenReturn(List.of());

        MyQuizList result = service.getMyQuizzes(MEMBER_ID, COURSE_ID);

        assertThat(result.completedCount()).isZero();
        assertThat(result.averageScore()).isZero();
        assertThat(result.quizzes()).hasSize(1);
        assertThat(result.quizzes().get(0).completed()).isFalse();
    }

    @Test
    void myQuizzesRejectWhenNotEnrolledInTheCourse() {
        when(enrollmentAccessPort.hasActiveEnrollment(MEMBER_ID, COURSE_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.getMyQuizzes(MEMBER_ID, COURSE_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUIZ_ENROLLMENT_REQUIRED);
    }

    @Test
    void myQuizzesReturnEmptyWhenCourseHasNoQuizzes() {
        when(enrollmentAccessPort.hasActiveEnrollment(MEMBER_ID, COURSE_ID)).thenReturn(true);
        when(quizRepository.findAllByCourseId(COURSE_ID)).thenReturn(List.of());
        when(courseTitlePort.findTitlesByCourseIds(anyCollection()))
                .thenReturn(Map.of(COURSE_ID, "React 완벽 가이드"));
        when(courseSectionTitlePort.findSectionsByIds(anyCollection())).thenReturn(Map.of());
        when(quizSubmissionRepository.findByMemberIdAndQuizIdIn(eq(MEMBER_ID), anyList()))
                .thenReturn(List.of());

        MyQuizList result = service.getMyQuizzes(MEMBER_ID, COURSE_ID);

        assertThat(result.completedCount()).isZero();
        assertThat(result.averageScore()).isZero();
        assertThat(result.quizzes()).isEmpty();
    }

    @Test
    void myQuizzesAverageAllCompleted() {
        Quiz week1 = quiz(90L, COURSE_ID, 100L, "1주", 10);
        Quiz week2 = quiz(91L, COURSE_ID, 200L, "2주", 10);
        when(enrollmentAccessPort.hasActiveEnrollment(MEMBER_ID, COURSE_ID)).thenReturn(true);
        when(quizRepository.findAllByCourseId(COURSE_ID)).thenReturn(List.of(week1, week2));
        when(courseTitlePort.findTitlesByCourseIds(anyCollection()))
                .thenReturn(Map.of(COURSE_ID, "React 완벽 가이드"));
        when(courseSectionTitlePort.findSectionsByIds(anyCollection())).thenReturn(Map.of(
                100L, new CourseSectionTitlePort.SectionInfo("섹션 1", 1),
                200L, new CourseSectionTitlePort.SectionInfo("섹션 2", 2)));
        when(quizSubmissionRepository.findByMemberIdAndQuizIdIn(eq(MEMBER_ID), anyList()))
                .thenReturn(List.of(
                        QuizSubmission.restore(55L, 90L, MEMBER_ID, 80, 10, 8,
                                LocalDateTime.of(2026, 5, 12, 0, 0), List.of()),
                        QuizSubmission.restore(56L, 91L, MEMBER_ID, 90, 10, 9,
                                LocalDateTime.of(2026, 5, 15, 0, 0), List.of())));

        MyQuizList result = service.getMyQuizzes(MEMBER_ID, COURSE_ID);

        assertThat(result.completedCount()).isEqualTo(2);
        assertThat(result.averageScore()).isEqualTo(85);
        assertThat(result.quizzes()).allSatisfy(item -> assertThat(item.completed()).isTrue());
    }

    // 2문항 퀴즈 헬퍼: Q1(id10) 정답=id102, Q2(id20) 정답=id201, 해설 보유
    private Quiz reportQuiz(Long id, Long sectionId, String title) {
        QuizQuestion q1 = QuizQuestion.restore(10L, 1, "질문1", "해설1", List.of(
                QuizOption.restore(101L, 1, "보기1", false),
                QuizOption.restore(102L, 2, "보기2", true),
                QuizOption.restore(103L, 3, "보기3", false),
                QuizOption.restore(104L, 4, "보기4", false)));
        QuizQuestion q2 = QuizQuestion.restore(20L, 2, "질문2", "해설2", List.of(
                QuizOption.restore(201L, 1, "보기1", true),
                QuizOption.restore(202L, 2, "보기2", false),
                QuizOption.restore(203L, 3, "보기3", false),
                QuizOption.restore(204L, 4, "보기4", false)));
        return Quiz.restore(id, INSTRUCTOR_ID, COURSE_ID, sectionId, title,
                List.of(q1, q2), LocalDateTime.of(2026, 5, 10, 15, 30));
    }

    private Quiz studentQuiz() {
        return reportQuiz(90L, SECTION_ID, "React 기초 개념 퀴즈");
    }

    @Test
    void quizReportComputesScoreDiffAgainstPreviousWeekAndSeparatesWrongNotes() {
        Quiz current = reportQuiz(90L, 200L, "2주차 퀴즈");   // week 2
        Quiz previous = reportQuiz(91L, 100L, "1주차 퀴즈");  // week 1
        when(quizRepository.findById(90L)).thenReturn(Optional.of(current));
        when(enrollmentAccessPort.hasActiveEnrollment(MEMBER_ID, COURSE_ID)).thenReturn(true);
        when(quizRepository.findAllByCourseId(COURSE_ID)).thenReturn(List.of(current, previous));
        when(courseSectionTitlePort.findSectionsByIds(anyCollection())).thenReturn(Map.of(
                200L, new CourseSectionTitlePort.SectionInfo("섹션 2", 2),
                100L, new CourseSectionTitlePort.SectionInfo("섹션 1", 1)));
        // 현재(quiz90): Q1 정답(102), Q2 오답(202) → score 60 / 정답1 오답1
        QuizSubmission currentSub = QuizSubmission.restore(55L, 90L, MEMBER_ID, 60, 2, 1,
                LocalDateTime.of(2026, 5, 12, 0, 0),
                List.of(
                        QuizSubmissionAnswer.restore(1L, 10L, 102L, true),
                        QuizSubmissionAnswer.restore(2L, 20L, 202L, false)));
        // 이전 주차(quiz91): score 75
        QuizSubmission prevSub = QuizSubmission.restore(56L, 91L, MEMBER_ID, 75, 2, 2,
                LocalDateTime.of(2026, 5, 5, 0, 0), List.of());
        when(quizSubmissionRepository.findByMemberIdAndQuizIdIn(eq(MEMBER_ID), anyList()))
                .thenReturn(List.of(currentSub, prevSub));

        QuizReport report = service.getMyQuizReport(MEMBER_ID, 90L);

        assertThat(report.quizId()).isEqualTo(90L);
        assertThat(report.week()).isEqualTo(2);
        assertThat(report.score()).isEqualTo(60);
        assertThat(report.totalScore()).isEqualTo(100);
        assertThat(report.correctCount()).isEqualTo(1);
        assertThat(report.incorrectCount()).isEqualTo(1);
        // 이전 주차 75점 대비 -15, previousScore로 이전 점수 노출
        assertThat(report.scoreDiff()).isEqualTo(-15);
        assertThat(report.previousScore()).isEqualTo(75);
        assertThat(report.questions()).hasSize(2);
        // 오답노트는 틀린 문항(Q2)만
        assertThat(report.wrongNotes()).hasSize(1);
        QuizReport.QuestionResult wrong = report.wrongNotes().get(0);
        assertThat(wrong.questionId()).isEqualTo(20L);
        assertThat(wrong.correct()).isFalse();
        assertThat(wrong.correctOptionId()).isEqualTo(201L);
        assertThat(wrong.selectedOptionId()).isEqualTo(202L);
        assertThat(wrong.explanation()).isEqualTo("해설2");
    }

    @Test
    void quizReportScoreDiffIsZeroWhenNoPreviousWeekSubmission() {
        Quiz current = reportQuiz(90L, 100L, "1주차 퀴즈");   // week 1, 이전 없음
        when(quizRepository.findById(90L)).thenReturn(Optional.of(current));
        when(enrollmentAccessPort.hasActiveEnrollment(MEMBER_ID, COURSE_ID)).thenReturn(true);
        when(quizRepository.findAllByCourseId(COURSE_ID)).thenReturn(List.of(current));
        when(courseSectionTitlePort.findSectionsByIds(anyCollection()))
                .thenReturn(Map.of(100L, new CourseSectionTitlePort.SectionInfo("섹션 1", 1)));
        QuizSubmission currentSub = QuizSubmission.restore(55L, 90L, MEMBER_ID, 60, 2, 1,
                LocalDateTime.of(2026, 5, 12, 0, 0),
                List.of(
                        QuizSubmissionAnswer.restore(1L, 10L, 102L, true),
                        QuizSubmissionAnswer.restore(2L, 20L, 202L, false)));
        when(quizSubmissionRepository.findByMemberIdAndQuizIdIn(eq(MEMBER_ID), anyList()))
                .thenReturn(List.of(currentSub));

        QuizReport report = service.getMyQuizReport(MEMBER_ID, 90L);

        // 이전 주차 제출 없음 → previousScore null (scoreDiff 0과 구분)
        assertThat(report.previousScore()).isNull();
        assertThat(report.scoreDiff()).isZero();
    }

    @Test
    void quizReportRejectsWhenQuizDoesNotExist() {
        when(quizRepository.findById(90L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyQuizReport(MEMBER_ID, 90L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUIZ_NOT_FOUND);
    }

    @Test
    void quizReportRejectsWhenNotSubmitted() {
        Quiz current = reportQuiz(90L, 100L, "1주차 퀴즈");
        when(quizRepository.findById(90L)).thenReturn(Optional.of(current));
        when(enrollmentAccessPort.hasActiveEnrollment(MEMBER_ID, COURSE_ID)).thenReturn(true);
        when(quizRepository.findAllByCourseId(COURSE_ID)).thenReturn(List.of(current));
        when(courseSectionTitlePort.findSectionsByIds(anyCollection()))
                .thenReturn(Map.of(100L, new CourseSectionTitlePort.SectionInfo("섹션 1", 1)));
        when(quizSubmissionRepository.findByMemberIdAndQuizIdIn(eq(MEMBER_ID), anyList()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.getMyQuizReport(MEMBER_ID, 90L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUIZ_SUBMISSION_NOT_FOUND);
    }

    @Test
    void quizReportRejectsWhenNotEnrolled() {
        Quiz current = reportQuiz(90L, 100L, "1주차 퀴즈");
        when(quizRepository.findById(90L)).thenReturn(Optional.of(current));
        when(enrollmentAccessPort.hasActiveEnrollment(MEMBER_ID, COURSE_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.getMyQuizReport(MEMBER_ID, 90L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUIZ_ENROLLMENT_REQUIRED);
    }

    @Test
    void studentQuizDetailReturnsQuestionsWithoutAnswerInfoWhenNotSubmitted() {
        when(quizRepository.findById(90L)).thenReturn(Optional.of(studentQuiz()));
        when(enrollmentAccessPort.hasActiveEnrollment(MEMBER_ID, COURSE_ID)).thenReturn(true);
        when(courseTitlePort.findTitlesByCourseIds(anyCollection()))
                .thenReturn(Map.of(COURSE_ID, "React 완벽 가이드"));
        when(courseSectionTitlePort.findTitlesBySectionIds(anyCollection()))
                .thenReturn(Map.of(SECTION_ID, "섹션 1: React 기초"));
        when(quizSubmissionRepository.findByMemberIdAndQuizIdIn(eq(MEMBER_ID), anyList()))
                .thenReturn(List.of());

        StudentQuizDetail detail = service.getStudentQuizDetail(MEMBER_ID, 90L);

        assertThat(detail.quizId()).isEqualTo(90L);
        assertThat(detail.courseTitle()).isEqualTo("React 완벽 가이드");
        assertThat(detail.sectionTitle()).isEqualTo("섹션 1: React 기초");
        assertThat(detail.totalQuestionCount()).isEqualTo(2);
        assertThat(detail.submitted()).isFalse();
        assertThat(detail.answeredCount()).isZero();
        // 응시 화면 DTO에는 정답/해설 필드 자체가 없어야 한다 (컴파일 타임 보장) — 값 노출만 재확인
        assertThat(detail.questions()).hasSize(2);
        assertThat(detail.questions().get(0).options()).hasSize(4);
        assertThat(detail.questions().get(0).options())
                .allSatisfy(o -> assertThat(o.optionText()).isNotBlank());
    }

    @Test
    void studentQuizDetailReflectsSubmittedStateAndAnsweredCount() {
        // 2문항 중 1문항만 응답한 제출 이력
        QuizSubmission submission = QuizSubmission.restore(55L, 90L, MEMBER_ID, 50, 2, 1,
                LocalDateTime.of(2026, 5, 12, 0, 0),
                List.of(
                        QuizSubmissionAnswer.restore(1L, 10L, 102L, true),
                        QuizSubmissionAnswer.restore(2L, 20L, null, false)));
        when(quizRepository.findById(90L)).thenReturn(Optional.of(studentQuiz()));
        when(enrollmentAccessPort.hasActiveEnrollment(MEMBER_ID, COURSE_ID)).thenReturn(true);
        when(courseTitlePort.findTitlesByCourseIds(anyCollection()))
                .thenReturn(Map.of(COURSE_ID, "React 완벽 가이드"));
        when(courseSectionTitlePort.findTitlesBySectionIds(anyCollection()))
                .thenReturn(Map.of(SECTION_ID, "섹션 1: React 기초"));
        when(quizSubmissionRepository.findByMemberIdAndQuizIdIn(eq(MEMBER_ID), anyList()))
                .thenReturn(List.of(submission));

        StudentQuizDetail detail = service.getStudentQuizDetail(MEMBER_ID, 90L);

        assertThat(detail.submitted()).isTrue();
        assertThat(detail.answeredCount()).isEqualTo(1);
    }

    @Test
    void studentQuizDetailRejectsWhenQuizDoesNotExist() {
        when(quizRepository.findById(90L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStudentQuizDetail(MEMBER_ID, 90L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUIZ_NOT_FOUND);
    }

    @Test
    void studentQuizDetailRejectsWhenNotEnrolled() {
        when(quizRepository.findById(90L)).thenReturn(Optional.of(studentQuiz()));
        when(enrollmentAccessPort.hasActiveEnrollment(MEMBER_ID, COURSE_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.getStudentQuizDetail(MEMBER_ID, 90L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUIZ_ENROLLMENT_REQUIRED);
    }

    private QuizStatisticsQuery statsQuery(QuizStatisticsQuery.SortType sort, QuizStatisticsQuery.FilterType filter) {
        return new QuizStatisticsQuery(INSTRUCTOR_ID, 90L, null, sort, filter, 0, 10);
    }

    private void stubStatsCommon() {
        Quiz quiz = reportQuiz(90L, SECTION_ID, "React 기초 개념 퀴즈");
        when(quizRepository.findById(90L)).thenReturn(Optional.of(quiz));
        when(courseTitlePort.findTitlesByCourseIds(anyCollection()))
                .thenReturn(Map.of(COURSE_ID, "React 완벽 가이드"));
        when(courseSectionTitlePort.findSectionsByIds(anyCollection()))
                .thenReturn(Map.of(SECTION_ID, new CourseSectionTitlePort.SectionInfo("1주차: React 기초", 1)));
        // 수강생 3명 (member1/2 응시, member3 미응시)
        when(courseStudentPort.findActiveStudents(COURSE_ID)).thenReturn(List.of(
                new CourseStudentPort.CourseStudent(1L, "choiaa", "최아"),
                new CourseStudentPort.CourseStudent(2L, "kimsu", "김수"),
                new CourseStudentPort.CourseStudent(3L, "leejin", "이진")));
        when(quizSubmissionRepository.findByQuizId(90L)).thenReturn(List.of(
                QuizSubmission.restore(55L, 90L, 1L, 90, 2, 2, LocalDateTime.of(2026, 5, 10, 0, 0), List.of()),
                QuizSubmission.restore(56L, 90L, 2L, 60, 2, 1, LocalDateTime.of(2026, 5, 12, 0, 0), List.of())));
    }

    @Test
    void instructorQuizStatisticsAggregatesSummaryDistributionAndStudents() {
        stubStatsCommon();

        InstructorQuizStatistics stats = service.getInstructorQuizStatistics(
                statsQuery(QuizStatisticsQuery.SortType.SCORE_DESC, QuizStatisticsQuery.FilterType.ALL));

        assertThat(stats.courseTitle()).isEqualTo("React 완벽 가이드");
        assertThat(stats.week()).isEqualTo(1);
        assertThat(stats.summary().totalCount()).isEqualTo(3);
        assertThat(stats.summary().submittedCount()).isEqualTo(2);
        assertThat(stats.summary().notSubmittedCount()).isEqualTo(1);
        assertThat(stats.summary().averageScore()).isEqualTo(75); // (90+60)/2

        // 분포: 90~100 1명(50%), 70~89 0명(0%), 50~69 1명(50%), 0~49 0명
        assertThat(stats.scoreDistribution()).hasSize(4);
        assertThat(stats.scoreDistribution().get(0).count()).isEqualTo(1);
        assertThat(stats.scoreDistribution().get(0).percentage()).isEqualTo(50);
        assertThat(stats.scoreDistribution().get(2).count()).isEqualTo(1);

        // SCORE_DESC 정렬: 90 → 60 → 미응시(null 뒤)
        assertThat(stats.students()).hasSize(3);
        assertThat(stats.students().get(0).score()).isEqualTo(90);
        assertThat(stats.students().get(1).score()).isEqualTo(60);
        assertThat(stats.students().get(2).submitted()).isFalse();
        assertThat(stats.students().get(2).score()).isNull();
        assertThat(stats.students().get(2).submittedAt()).isNull();
    }

    @Test
    void instructorQuizStatisticsFilterNotSubmittedReturnsOnlyNonSubmitters() {
        stubStatsCommon();

        InstructorQuizStatistics stats = service.getInstructorQuizStatistics(
                statsQuery(QuizStatisticsQuery.SortType.NAME, QuizStatisticsQuery.FilterType.NOT_SUBMITTED));

        assertThat(stats.students()).hasSize(1);
        assertThat(stats.students().get(0).userId()).isEqualTo("leejin");
        assertThat(stats.students().get(0).submitted()).isFalse();
    }

    @Test
    void instructorQuizStatisticsRejectsWhenNotOwner() {
        Quiz quiz = reportQuiz(90L, SECTION_ID, "React 기초 개념 퀴즈"); // instructorId = 1L
        when(quizRepository.findById(90L)).thenReturn(Optional.of(quiz));

        assertThatThrownBy(() -> service.getInstructorQuizStatistics(
                new QuizStatisticsQuery(999L, 90L, null,
                        QuizStatisticsQuery.SortType.SCORE_DESC, QuizStatisticsQuery.FilterType.ALL, 0, 10)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUIZ_NOT_AUTHORIZED);
    }

    @Test
    void instructorQuizStatisticsRejectsWhenQuizDoesNotExist() {
        when(quizRepository.findById(90L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getInstructorQuizStatistics(
                statsQuery(QuizStatisticsQuery.SortType.SCORE_DESC, QuizStatisticsQuery.FilterType.ALL)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUIZ_NOT_FOUND);
    }

    @Test
    void instructorQuizStatisticsPaginatesStudents() {
        stubStatsCommon(); // 수강생 3명

        // page=1, size=2 → 3명 중 2번째 페이지엔 1명만
        InstructorQuizStatistics stats = service.getInstructorQuizStatistics(
                new QuizStatisticsQuery(INSTRUCTOR_ID, 90L, null,
                        QuizStatisticsQuery.SortType.SCORE_DESC, QuizStatisticsQuery.FilterType.ALL, 1, 2));

        assertThat(stats.students()).hasSize(1);
        // 요약은 페이지와 무관하게 전체 기준
        assertThat(stats.summary().totalCount()).isEqualTo(3);
        assertThat(stats.summary().submittedCount()).isEqualTo(2);
    }

    @Test
    void instructorQuizStatisticsBreaksScoreTiesByUserIdDeterministically() {
        Quiz quiz = reportQuiz(90L, SECTION_ID, "React 기초 개념 퀴즈");
        when(quizRepository.findById(90L)).thenReturn(Optional.of(quiz));
        when(courseTitlePort.findTitlesByCourseIds(anyCollection()))
                .thenReturn(Map.of(COURSE_ID, "React 완벽 가이드"));
        when(courseSectionTitlePort.findSectionsByIds(anyCollection()))
                .thenReturn(Map.of(SECTION_ID, new CourseSectionTitlePort.SectionInfo("1주차", 1)));
        // 세 명 모두 동점(80) — userId 오름차순(aaa, bbb, ccc)으로 확정돼야 함
        when(courseStudentPort.findActiveStudents(COURSE_ID)).thenReturn(List.of(
                new CourseStudentPort.CourseStudent(3L, "ccc", "병"),
                new CourseStudentPort.CourseStudent(1L, "aaa", "갑"),
                new CourseStudentPort.CourseStudent(2L, "bbb", "을")));
        when(quizSubmissionRepository.findByQuizId(90L)).thenReturn(List.of(
                QuizSubmission.restore(51L, 90L, 1L, 80, 2, 2, LocalDateTime.of(2026, 5, 10, 0, 0), List.of()),
                QuizSubmission.restore(52L, 90L, 2L, 80, 2, 2, LocalDateTime.of(2026, 5, 10, 0, 0), List.of()),
                QuizSubmission.restore(53L, 90L, 3L, 80, 2, 2, LocalDateTime.of(2026, 5, 10, 0, 0), List.of())));

        InstructorQuizStatistics stats = service.getInstructorQuizStatistics(
                statsQuery(QuizStatisticsQuery.SortType.SCORE_DESC, QuizStatisticsQuery.FilterType.ALL));

        assertThat(stats.students()).extracting(InstructorQuizStatistics.StudentScore::userId)
                .containsExactly("aaa", "bbb", "ccc");
    }

    @Test
    void instructorQuizStatisticsReturnsZeroWhenNoStudents() {
        Quiz quiz = reportQuiz(90L, SECTION_ID, "React 기초 개념 퀴즈");
        when(quizRepository.findById(90L)).thenReturn(Optional.of(quiz));
        when(courseTitlePort.findTitlesByCourseIds(anyCollection()))
                .thenReturn(Map.of(COURSE_ID, "React 완벽 가이드"));
        when(courseSectionTitlePort.findSectionsByIds(anyCollection()))
                .thenReturn(Map.of(SECTION_ID, new CourseSectionTitlePort.SectionInfo("1주차", 1)));
        when(courseStudentPort.findActiveStudents(COURSE_ID)).thenReturn(List.of());
        when(quizSubmissionRepository.findByQuizId(90L)).thenReturn(List.of());

        InstructorQuizStatistics stats = service.getInstructorQuizStatistics(
                statsQuery(QuizStatisticsQuery.SortType.SCORE_DESC, QuizStatisticsQuery.FilterType.ALL));

        assertThat(stats.summary().totalCount()).isZero();
        assertThat(stats.summary().submittedCount()).isZero();
        assertThat(stats.summary().averageScore()).isZero();
        assertThat(stats.students()).isEmpty();
        // 분포 4구간은 유지하되 전부 0
        assertThat(stats.scoreDistribution()).hasSize(4);
        assertThat(stats.scoreDistribution()).allSatisfy(d -> {
            assertThat(d.count()).isZero();
            assertThat(d.percentage()).isZero();
        });
    }
}
