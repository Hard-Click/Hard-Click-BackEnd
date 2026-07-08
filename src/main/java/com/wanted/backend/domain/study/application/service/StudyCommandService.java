package com.wanted.backend.domain.study.application.service;

import com.wanted.backend.domain.study.application.command.CreateStudyCommand;
import com.wanted.backend.domain.study.application.port.ChatRoomCommandPort;
import com.wanted.backend.domain.study.application.result.StudyCreationResult;
import com.wanted.backend.domain.study.application.usecase.StudyCommandUseCase;
import com.wanted.backend.domain.study.domain.model.Study;
import com.wanted.backend.domain.study.domain.model.StudyParticipant;
import com.wanted.backend.domain.study.domain.repository.StudyParticipantRepository;
import com.wanted.backend.domain.study.domain.repository.StudyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StudyCommandService implements StudyCommandUseCase {

    private final StudyRepository studyRepository;
    private final StudyParticipantRepository studyParticipantRepository;
    private final ChatRoomCommandPort chatRoomCommandPort;

    public StudyCommandService(StudyRepository studyRepository,
                               StudyParticipantRepository studyParticipantRepository,
                               ChatRoomCommandPort chatRoomCommandPort) {
        this.studyRepository = studyRepository;
        this.studyParticipantRepository = studyParticipantRepository;
        this.chatRoomCommandPort = chatRoomCommandPort;
    }

    @Override
    public StudyCreationResult create(CreateStudyCommand command) {
        Study study = Study.create(
                command.hostId(), command.title(), command.subject(),
                command.maxCount(), command.content()
        );
        Study saved = studyRepository.save(study);

        studyParticipantRepository.save(StudyParticipant.create(saved.getId(), command.hostId()));

        Long chatRoomId = chatRoomCommandPort.createRoom(saved.getId(), command.hostId());

        return new StudyCreationResult(saved.getId(), chatRoomId);
    }
}
