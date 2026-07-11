package com.wanted.backend.domain.quiz.application.usecase;

import com.wanted.backend.domain.quiz.application.command.CreateAdminQuizCommand;
import com.wanted.backend.domain.quiz.application.command.CreateQuizCommand;
import com.wanted.backend.domain.quiz.application.command.DeleteQuizCommand;
import com.wanted.backend.domain.quiz.application.command.UpdateAdminQuizCommand;
import com.wanted.backend.domain.quiz.application.command.UpdateQuizCommand;

import java.util.List;

public interface QuizCommandUseCase {

    Long create(CreateQuizCommand command);

    // 관리자(ADMIN)용 — 대상 강의의 주인에게 귀속하여 등록 (인가는 컨트롤러 @PreAuthorize가 보장).
    Long createByAdmin(CreateAdminQuizCommand command);

    Long update(UpdateQuizCommand command);

    // 관리자(ADMIN)용 — 소유권 검증 없이 수정 (인가는 컨트롤러 @PreAuthorize가 보장).
    Long updateByAdmin(UpdateAdminQuizCommand command);

    void delete(DeleteQuizCommand command);

    // 관리자(ADMIN)용 — 소유권 검증 없이 삭제 (인가는 컨트롤러 @PreAuthorize가 보장).
    void deleteQuiz(Long quizId);

    // 섹션 삭제 cascade: 강의 수정으로 삭제된 섹션들의 퀴즈를 정리한다(SectionDeletedEvent 처리).
    void deleteBySectionIds(List<Long> sectionIds);
}
