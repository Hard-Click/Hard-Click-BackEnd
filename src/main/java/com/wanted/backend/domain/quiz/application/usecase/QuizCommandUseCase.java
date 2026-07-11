package com.wanted.backend.domain.quiz.application.usecase;

import com.wanted.backend.domain.quiz.application.command.CreateQuizCommand;
import com.wanted.backend.domain.quiz.application.command.DeleteQuizCommand;
import com.wanted.backend.domain.quiz.application.command.UpdateQuizCommand;

import java.util.List;

public interface QuizCommandUseCase {

    Long create(CreateQuizCommand command);

    Long update(UpdateQuizCommand command);

    void delete(DeleteQuizCommand command);

    // 섹션 삭제 cascade: 강의 수정으로 삭제된 섹션들의 퀴즈를 정리한다(SectionDeletedEvent 처리).
    void deleteBySectionIds(List<Long> sectionIds);
}
