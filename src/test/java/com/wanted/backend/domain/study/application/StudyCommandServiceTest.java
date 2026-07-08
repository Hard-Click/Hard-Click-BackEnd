package com.wanted.backend.domain.study.application;

import com.wanted.backend.domain.study.application.command.CreateStudyCommand;
import com.wanted.backend.domain.study.application.command.UpdateStudyCommand;
import com.wanted.backend.domain.study.application.port.ChatRoomCommandPort;
import com.wanted.backend.domain.study.application.result.StudyCreationResult;
import com.wanted.backend.domain.study.application.service.StudyCommandService;
import com.wanted.backend.domain.study.domain.model.Study;
import com.wanted.backend.domain.study.domain.model.StudyParticipant;
import com.wanted.backend.domain.study.domain.model.StudyStatus;
import com.wanted.backend.domain.study.domain.repository.StudyParticipantRepository;
import com.wanted.backend.domain.study.domain.repository.StudyRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StudyCommandServiceTest {

    @InjectMocks
    private StudyCommandService studyCommandService;

    @Mock
    private StudyRepository studyRepository;

    @Mock
    private StudyParticipantRepository studyParticipantRepository;

    @Mock
    private ChatRoomCommandPort chatRoomCommandPort;

    @Test
    @DisplayName("스터디 생성 시 방장이 참여자로 등록되고 채팅방이 함께 생성된다")
    void create_success() {
        // given
        CreateStudyCommand command = new CreateStudyCommand(1L, "제목", "MATH_1", 5, "내용");

        given(studyRepository.save(any(Study.class)))
                .willAnswer(invocation -> {
                    Study arg = invocation.getArgument(0);
                    return Study.restore(100L, arg.getHostId(), arg.getTitle(), arg.getSubject(),
                            arg.getContent(), arg.getMaxCount(), arg.getCurrentCount(), arg.getStatus(),
                            arg.getCreatedAt(), arg.getUpdatedAt());
                });
        given(chatRoomCommandPort.createRoom(100L, 1L)).willReturn(200L);

        // when
        StudyCreationResult result = studyCommandService.create(command);

        // then
        assertThat(result.studyId()).isEqualTo(100L);
        assertThat(result.chatRoomId()).isEqualTo(200L);

        ArgumentCaptor<StudyParticipant> captor = ArgumentCaptor.forClass(StudyParticipant.class);
        verify(studyParticipantRepository).save(captor.capture());
        assertThat(captor.getValue().getStudyId()).isEqualTo(100L);
        assertThat(captor.getValue().getMemberId()).isEqualTo(1L);

        verify(chatRoomCommandPort).createRoom(100L, 1L);
    }

    @Test
    @DisplayName("정원이 2명 미만이면 예외가 발생하고 저장이 호출되지 않는다")
    void create_fail_maxCountTooSmall() {
        // given
        CreateStudyCommand command = new CreateStudyCommand(1L, "제목", "MATH_1", 1, "내용");

        // when & then
        assertThatThrownBy(() -> studyCommandService.create(command))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.STUDY_MIN_COUNT_INVALID.getMessage());

        verify(studyRepository, never()).save(any());
        verify(studyParticipantRepository, never()).save(any());
        verify(chatRoomCommandPort, never()).createRoom(any(), any());
    }

    @Test
    @DisplayName("채팅방 생성이 실패하면 예외가 그대로 전파된다")
    void create_fail_chatRoomCreationFails() {
        // given
        CreateStudyCommand command = new CreateStudyCommand(1L, "제목", "MATH_1", 5, "내용");

        given(studyRepository.save(any(Study.class)))
                .willAnswer(invocation -> {
                    Study arg = invocation.getArgument(0);
                    return Study.restore(100L, arg.getHostId(), arg.getTitle(), arg.getSubject(),
                            arg.getContent(), arg.getMaxCount(), arg.getCurrentCount(), arg.getStatus(),
                            arg.getCreatedAt(), arg.getUpdatedAt());
                });
        given(chatRoomCommandPort.createRoom(100L, 1L)).willThrow(new RuntimeException("chat room creation failed"));

        // when & then
        assertThatThrownBy(() -> studyCommandService.create(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("chat room creation failed");
    }

    private Study activeStudy() {
        return Study.restore(45L, 1L, "수학 1등급 목표 스터디", "MATH_1",
                "매주 일요일 밤 10시에 모여서 질문 받습니다.", 5, 3, StudyStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("방장이 수정하면 정상적으로 반영된다")
    void update_success() {
        // given
        given(studyRepository.findById(45L)).willReturn(Optional.of(activeStudy()));

        // when
        studyCommandService.update(new UpdateStudyCommand(45L, 1L, "수정된 제목", "MATH_2", 6, "수정된 내용"));

        // then
        ArgumentCaptor<Study> captor = ArgumentCaptor.forClass(Study.class);
        verify(studyRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("수정된 제목");
        assertThat(captor.getValue().getSubject()).isEqualTo("MATH_2");
        assertThat(captor.getValue().getMaxCount()).isEqualTo(6);
        assertThat(captor.getValue().getContent()).isEqualTo("수정된 내용");
    }

    @Test
    @DisplayName("존재하지 않는 스터디를 수정하려 하면 예외가 발생한다")
    void update_fail_notFound() {
        // given
        given(studyRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> studyCommandService.update(new UpdateStudyCommand(999L, 1L, "제목", "MATH_1", 5, "내용")))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.STUDY_NOT_FOUND.getMessage());

        verify(studyRepository, never()).save(any());
    }

    @Test
    @DisplayName("방장이 아닌 회원이 수정하려 하면 예외가 발생한다")
    void update_fail_notOwner() {
        // given
        given(studyRepository.findById(45L)).willReturn(Optional.of(activeStudy()));

        // when & then
        assertThatThrownBy(() -> studyCommandService.update(new UpdateStudyCommand(45L, 999L, "제목", "MATH_1", 5, "내용")))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.STUDY_UPDATE_FORBIDDEN.getMessage());

        verify(studyRepository, never()).save(any());
    }

    @Test
    @DisplayName("정원을 현재 인원 미만으로 줄이면 예외가 발생한다")
    void update_fail_maxCountBelowCurrent() {
        // given
        given(studyRepository.findById(45L)).willReturn(Optional.of(activeStudy()));

        // when & then
        assertThatThrownBy(() -> studyCommandService.update(new UpdateStudyCommand(45L, 1L, "제목", "MATH_1", 2, "내용")))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.STUDY_MAX_COUNT_BELOW_CURRENT.getMessage());

        verify(studyRepository, never()).save(any());
    }
}
