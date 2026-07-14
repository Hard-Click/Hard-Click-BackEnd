package com.wanted.backend.domain.quiz.application.command;

import java.util.List;

// 관리자 퀴즈 수정 커맨드. 소유권(instructor) 검증 없이 강의/섹션·제목·문항을 수정한다.
public record UpdateAdminQuizCommand(
        Long quizId,
        Long courseId,
        Long sectionId,
        String quizTitle,
        List<QuizQuestionCommand> questions
) {}
