package com.wanted.backend.domain.study.infrastructure.chat;

import com.wanted.backend.domain.chat.application.port.StudyInfoQueryPort;
import com.wanted.backend.domain.chat.application.port.StudyInfoResult;
import com.wanted.backend.domain.study.domain.repository.StudyRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class StudyInfoQueryPortAdapter implements StudyInfoQueryPort {

    private final StudyRepository studyRepository;

    public StudyInfoQueryPortAdapter(StudyRepository studyRepository) {
        this.studyRepository = studyRepository;
    }

    @Override
    public Optional<StudyInfoResult> getStudyInfo(Long studyId) {
        return studyRepository.findById(studyId)
                .map(study -> new StudyInfoResult(study.getTitle(), study.getSubject()));
    }
}
