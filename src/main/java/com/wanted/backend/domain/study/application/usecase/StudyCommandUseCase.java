package com.wanted.backend.domain.study.application.usecase;

import com.wanted.backend.domain.study.application.command.CreateStudyCommand;
import com.wanted.backend.domain.study.application.command.UpdateStudyCommand;
import com.wanted.backend.domain.study.application.result.StudyCreationResult;

public interface StudyCommandUseCase {
    StudyCreationResult create(CreateStudyCommand command);

    void update(UpdateStudyCommand command);
}
