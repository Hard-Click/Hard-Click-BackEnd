package com.wanted.backend.domain.chat.infrastructure.websocket;

import com.wanted.backend.domain.chat.application.port.MemberNamePort;
import com.wanted.backend.domain.chat.domain.repository.ChatRoomParticipantRepository;
import com.wanted.backend.domain.chat.infrastructure.websocket.message.ParticipantPresenceMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ChatParticipantPresenceResolverTest {

    @InjectMocks
    private ChatParticipantPresenceResolver resolver;

    @Mock
    private ChatRoomParticipantRepository chatRoomParticipantRepository;

    @Mock
    private MemberNamePort memberNamePort;

    @Mock
    private ChatPresenceTracker presenceTracker;

    @Test
    @DisplayName("참여자 목록과 이름, 온라인 여부를 함께 조합해서 반환한다")
    void resolve_success() {
        // given
        given(chatRoomParticipantRepository.findMemberIdsByChatRoomId(12L)).willReturn(List.of(1L, 2L));
        given(memberNamePort.getNamesByMemberIds(any())).willReturn(Map.of(1L, "이지연", 2L, "김민수"));
        given(presenceTracker.getOnlineMemberIds(12L)).willReturn(Set.of(1L));

        // when
        List<ParticipantPresenceMessage> result = resolver.resolve(12L);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                new ParticipantPresenceMessage(1L, "이*연", true),
                new ParticipantPresenceMessage(2L, "김*수", false));
    }

    @Test
    @DisplayName("이름 조회가 실패하면 알 수 없음으로 대체된다")
    void resolve_success_nameResolutionFails() {
        // given
        given(chatRoomParticipantRepository.findMemberIdsByChatRoomId(12L)).willReturn(List.of(1L));
        given(memberNamePort.getNamesByMemberIds(any())).willThrow(new RuntimeException("timeout"));
        given(presenceTracker.getOnlineMemberIds(12L)).willReturn(Set.of());

        // when
        List<ParticipantPresenceMessage> result = resolver.resolve(12L);

        // then
        assertThat(result).containsExactly(new ParticipantPresenceMessage(1L, "알 수 없음", false));
    }
}
