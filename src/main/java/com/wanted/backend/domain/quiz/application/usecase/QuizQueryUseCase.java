package com.wanted.backend.domain.quiz.application.usecase;

import com.wanted.backend.domain.quiz.application.query.QuizStatisticsQuery;
import com.wanted.backend.domain.quiz.application.result.AdminCourseQuizzes;
import com.wanted.backend.domain.quiz.application.result.InstructorQuizDetail;
import com.wanted.backend.domain.quiz.application.result.InstructorQuizStatistics;
import com.wanted.backend.domain.quiz.application.result.InstructorQuizSummary;
import com.wanted.backend.domain.quiz.application.result.MyQuizList;
import com.wanted.backend.domain.quiz.application.result.QuizReport;
import com.wanted.backend.domain.quiz.application.result.StudentQuizDetail;

import java.util.List;

public interface QuizQueryUseCase {

    List<InstructorQuizSummary> getInstructorQuizzes(Long instructorId, Long courseId, Long sectionId);

    InstructorQuizDetail getInstructorQuizDetail(Long instructorId, Long quizId);

    // 관리자(ADMIN)용 — 소유권 검증 없이 상세 조회 (인가는 컨트롤러 @PreAuthorize가 보장).
    InstructorQuizDetail getDetailByAdmin(Long quizId);

    MyQuizList getMyQuizzes(Long memberId, Long courseId);

    StudentQuizDetail getStudentQuizDetail(Long memberId, Long quizId);

    QuizReport getMyQuizReport(Long memberId, Long quizId);

    InstructorQuizStatistics getInstructorQuizStatistics(QuizStatisticsQuery query);

    // 관리자(ADMIN)용 — 소유권 검증 없이 통계 조회 (인가는 컨트롤러 @PreAuthorize가 보장).
    InstructorQuizStatistics getStatisticsByAdmin(QuizStatisticsQuery query);

    // 관리자(ADMIN)용 — 특정 강의의 주차별 퀴즈 목록 (sectionId로 특정 주차만 필터 가능).
    AdminCourseQuizzes getCourseQuizzesByAdmin(Long courseId, Long sectionId);
}
