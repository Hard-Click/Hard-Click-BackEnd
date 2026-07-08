package com.wanted.backend.domain.quiz.application.usecase;

import com.wanted.backend.domain.quiz.application.result.InstructorQuizDetail;
import com.wanted.backend.domain.quiz.application.result.InstructorQuizSummary;
import com.wanted.backend.domain.quiz.application.result.MyQuizList;
import com.wanted.backend.domain.quiz.application.result.QuizReport;
import com.wanted.backend.domain.quiz.application.result.StudentQuizDetail;

import java.util.List;

public interface QuizQueryUseCase {

    List<InstructorQuizSummary> getInstructorQuizzes(Long instructorId, Long courseId, Long sectionId);

    InstructorQuizDetail getInstructorQuizDetail(Long instructorId, Long quizId);

    MyQuizList getMyQuizzes(Long memberId, Long courseId);

    StudentQuizDetail getStudentQuizDetail(Long memberId, Long quizId);

    QuizReport getMyQuizReport(Long memberId, Long quizId);
}
