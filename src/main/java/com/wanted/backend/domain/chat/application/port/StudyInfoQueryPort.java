package com.wanted.backend.domain.chat.application.port;

import java.util.Optional;

public interface StudyInfoQueryPort {
    Optional<StudyInfoResult> getStudyInfo(Long studyId);
}
