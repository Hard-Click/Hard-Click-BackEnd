package com.wanted.backend.domain.study.application.service;

import com.wanted.backend.domain.study.application.command.CreateStudyCommand;
import com.wanted.backend.domain.study.application.command.DeleteStudyCommand;
import com.wanted.backend.domain.study.application.command.JoinStudyCommand;
import com.wanted.backend.domain.study.application.command.KickStudyMemberCommand;
import com.wanted.backend.domain.study.application.command.LeaveStudyCommand;
import com.wanted.backend.domain.study.application.command.UpdateStudyCommand;
import com.wanted.backend.domain.study.application.event.StudyClosedEvent;
import com.wanted.backend.domain.study.application.event.StudyJoinedEvent;
import com.wanted.backend.domain.study.application.event.StudyKickedEvent;
import com.wanted.backend.domain.study.application.event.StudyLeftEvent;
import com.wanted.backend.domain.study.application.port.ChatRoomCommandPort;
import com.wanted.backend.domain.study.application.port.ChatRoomQueryPort;
import com.wanted.backend.domain.study.application.result.JoinStudyResult;
import com.wanted.backend.domain.study.application.result.StudyCreationResult;
import com.wanted.backend.domain.study.application.usecase.StudyCommandUseCase;
import com.wanted.backend.domain.study.domain.model.Study;
import com.wanted.backend.domain.study.domain.model.StudyBannedMember;
import com.wanted.backend.domain.study.domain.model.StudyParticipant;
import com.wanted.backend.domain.study.domain.repository.StudyBannedMemberRepository;
import com.wanted.backend.domain.study.domain.repository.StudyParticipantRepository;
import com.wanted.backend.domain.study.domain.repository.StudyRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StudyCommandService implements StudyCommandUseCase {

    private final StudyRepository studyRepository;
    private final StudyParticipantRepository studyParticipantRepository;
    private final StudyBannedMemberRepository studyBannedMemberRepository;
    private final ChatRoomCommandPort chatRoomCommandPort;
    private final ChatRoomQueryPort chatRoomQueryPort;
    private final ApplicationEventPublisher eventPublisher;

    public StudyCommandService(StudyRepository studyRepository,
                               StudyParticipantRepository studyParticipantRepository,
                               StudyBannedMemberRepository studyBannedMemberRepository,
                               ChatRoomCommandPort chatRoomCommandPort,
                               ChatRoomQueryPort chatRoomQueryPort,
                               ApplicationEventPublisher eventPublisher) {
        this.studyRepository = studyRepository;
        this.studyParticipantRepository = studyParticipantRepository;
        this.studyBannedMemberRepository = studyBannedMemberRepository;
        this.chatRoomCommandPort = chatRoomCommandPort;
        this.chatRoomQueryPort = chatRoomQueryPort;
        this.eventPublisher = eventPublisher;
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

    @Override
    public void update(UpdateStudyCommand command) {
        Study study = studyRepository.findById(command.groupId())
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDY_NOT_FOUND));

        study.update(command.memberId(), command.title(), command.subject(), command.maxCount(), command.content());

        studyRepository.save(study);
    }

    @Override
    public JoinStudyResult join(JoinStudyCommand command) {
        Study study = studyRepository.findByIdForUpdate(command.groupId())
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDY_NOT_FOUND));

        if (studyBannedMemberRepository.existsByStudyIdAndMemberId(command.groupId(), command.memberId())) {
            throw new BusinessException(ErrorCode.STUDY_KICKED);
        }

        if (studyParticipantRepository.existsByStudyIdAndMemberId(command.groupId(), command.memberId())) {
            throw new BusinessException(ErrorCode.STUDY_ALREADY_JOINED);
        }

        study.join();
        Study saved = studyRepository.save(study);

        studyParticipantRepository.save(StudyParticipant.create(command.groupId(), command.memberId()));

        Long chatRoomId = chatRoomQueryPort.findChatRoomIdByStudyId(command.groupId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        chatRoomCommandPort.addParticipant(chatRoomId, command.memberId());

        eventPublisher.publishEvent(new StudyJoinedEvent(chatRoomId, saved.getId(), command.memberId(), saved.getCurrentCount()));

        return new JoinStudyResult(saved.getId(), chatRoomId, saved.getCurrentCount());
    }

    @Override
    public void delete(DeleteStudyCommand command) {
        Study study = studyRepository.findByIdForUpdate(command.groupId())
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDY_NOT_FOUND));

        study.validateDeletable(command.memberId());
        study.close();
        studyRepository.save(study);

        Long chatRoomId = chatRoomQueryPort.findChatRoomIdByStudyId(command.groupId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        chatRoomCommandPort.closeRoom(chatRoomId);

        eventPublisher.publishEvent(new StudyClosedEvent(chatRoomId, command.groupId()));
    }

    @Override
    public void leave(LeaveStudyCommand command) {
        Study study = studyRepository.findByIdForUpdate(command.groupId())
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDY_NOT_FOUND));

        if (!studyParticipantRepository.existsByStudyIdAndMemberId(command.groupId(), command.memberId())) {
            throw new BusinessException(ErrorCode.STUDY_NOT_JOINED);
        }

        study.leave(command.memberId());
        Study saved = studyRepository.save(study);

        studyParticipantRepository.deleteByStudyIdAndMemberId(command.groupId(), command.memberId());

        Long chatRoomId = chatRoomQueryPort.findChatRoomIdByStudyId(command.groupId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        chatRoomCommandPort.removeParticipant(chatRoomId, command.memberId());

        eventPublisher.publishEvent(new StudyLeftEvent(chatRoomId, saved.getId(), command.memberId(), saved.getCurrentCount()));
    }

    @Override
    public void kick(KickStudyMemberCommand command) {
        Study study = studyRepository.findByIdForUpdate(command.groupId())
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDY_NOT_FOUND));

        study.validateKickable(command.hostId(), command.targetMemberId());

        if (!studyParticipantRepository.existsByStudyIdAndMemberId(command.groupId(), command.targetMemberId())) {
            throw new BusinessException(ErrorCode.STUDY_TARGET_NOT_JOINED);
        }

        study.kick();
        Study saved = studyRepository.save(study);

        studyParticipantRepository.deleteByStudyIdAndMemberId(command.groupId(), command.targetMemberId());
        studyBannedMemberRepository.save(StudyBannedMember.create(command.groupId(), command.targetMemberId()));

        Long chatRoomId = chatRoomQueryPort.findChatRoomIdByStudyId(command.groupId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        chatRoomCommandPort.removeParticipant(chatRoomId, command.targetMemberId());

        eventPublisher.publishEvent(new StudyKickedEvent(chatRoomId, saved.getId(), command.targetMemberId(), saved.getCurrentCount()));
    }
}
