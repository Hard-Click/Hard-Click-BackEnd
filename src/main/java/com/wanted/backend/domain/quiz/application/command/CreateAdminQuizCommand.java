package com.wanted.backend.domain.quiz.application.command;

import java.util.List;

// 관리자 퀴즈 등록 커맨드. instructor_id는 요청이 아니라 '대상 강의의 주인'에서 파생하므로 담지 않는다.
public record CreateAdminQuizCommand(
        Long courseId,
        Long sectionId,
        String quizTitle,
        List<QuizQuestionCommand> questions
) {}
