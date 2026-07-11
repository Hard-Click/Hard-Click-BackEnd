package com.wanted.backend.domain.quiz.presentation;

import com.wanted.backend.domain.quiz.application.command.QuizQuestionCommand;
import com.wanted.backend.domain.quiz.presentation.request.InstructorQuizRequest;

import java.util.List;

// 강사/관리자 퀴즈 등록·수정 요청의 문항을 애플리케이션 커맨드로 변환하는 공용 매퍼.
final class QuizRequestMapper {

    private QuizRequestMapper() {
    }

    static List<QuizQuestionCommand> toQuestionCommands(List<InstructorQuizRequest.Question> questions) {
        return questions.stream()
                .map(q -> new QuizQuestionCommand(
                        q.questionText(),
                        q.explanation(),
                        q.correctOptionNumber(),
                        q.options().stream().map(InstructorQuizRequest.Option::optionText).toList()))
                .toList();
    }
}
