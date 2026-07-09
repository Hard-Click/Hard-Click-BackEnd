package com.wanted.backend.domain.study.application.usecase;

import com.wanted.backend.domain.study.application.command.CreateStudyCommand;
import com.wanted.backend.domain.study.application.command.DeleteStudyCommand;
import com.wanted.backend.domain.study.application.command.JoinStudyCommand;
import com.wanted.backend.domain.study.application.command.LeaveStudyCommand;
import com.wanted.backend.domain.study.application.command.UpdateStudyCommand;
import com.wanted.backend.domain.study.application.result.JoinStudyResult;
import com.wanted.backend.domain.study.application.result.StudyCreationResult;

public interface StudyCommandUseCase {
    StudyCreationResult create(CreateStudyCommand command);

    void update(UpdateStudyCommand command);

    JoinStudyResult join(JoinStudyCommand command);

    void delete(DeleteStudyCommand command);

    void leave(LeaveStudyCommand command);
}
