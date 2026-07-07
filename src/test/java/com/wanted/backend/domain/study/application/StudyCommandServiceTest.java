package com.wanted.backend.domain.study.application;

import com.wanted.backend.domain.study.application.command.CreateStudyCommand;
import com.wanted.backend.domain.study.application.port.ChatRoomCommandPort;
import com.wanted.backend.domain.study.application.result.StudyCreationResult;
import com.wanted.backend.domain.study.application.service.StudyCommandService;
import com.wanted.backend.domain.study.domain.model.Study;
import com.wanted.backend.domain.study.domain.model.StudyParticipant;
import com.wanted.backend.domain.study.domain.repository.StudyParticipantRepository;
import com.wanted.backend.domain.study.domain.repository.StudyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
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
}
