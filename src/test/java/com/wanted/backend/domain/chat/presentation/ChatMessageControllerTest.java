package com.wanted.backend.domain.chat.presentation;

import com.wanted.backend.domain.chat.application.command.SendMessageCommand;
import com.wanted.backend.domain.chat.application.usecase.ChatMessageCommandUseCase;
import com.wanted.backend.domain.chat.application.usecase.ChatTypingUseCase;
import com.wanted.backend.domain.chat.infrastructure.websocket.ChatPrincipal;
import com.wanted.backend.domain.chat.presentation.request.SendMessageRequest;
import com.wanted.backend.domain.chat.presentation.response.ChatErrorMessage;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatMessageControllerTest {

    @Mock
    private ChatMessageCommandUseCase chatMessageCommandUseCase;

    @Mock
    private ChatTypingUseCase chatTypingUseCase;

    private ChatMessageController controller;

    @Test
    @DisplayName("메시지 전송 시 Principal의 memberId로 커맨드를 만들어 위임한다")
    void sendMessage_delegatesWithSenderIdFromPrincipal() {
        controller = new ChatMessageController(chatMessageCommandUseCase, chatTypingUseCase);

        controller.sendMessage(45L, new SendMessageRequest("안녕하세요"), new ChatPrincipal(1L));

        ArgumentCaptor<SendMessageCommand> captor = ArgumentCaptor.forClass(SendMessageCommand.class);
        verify(chatMessageCommandUseCase).send(captor.capture());
        assertThat(captor.getValue().chatRoomId()).isEqualTo(45L);
        assertThat(captor.getValue().senderId()).isEqualTo(1L);
        assertThat(captor.getValue().content()).isEqualTo("안녕하세요");
    }

    @Test
    @DisplayName("타이핑 알림 시 Principal의 memberId로 위임한다")
    void notifyTyping_delegatesWithMemberIdFromPrincipal() {
        controller = new ChatMessageController(chatMessageCommandUseCase, chatTypingUseCase);

        controller.notifyTyping(45L, new ChatPrincipal(1L));

        verify(chatTypingUseCase).notifyTyping(45L, 1L);
    }

    @Test
    @DisplayName("인증되지 않은 Principal이면 예외가 발생한다")
    void sendMessage_fail_unauthenticatedPrincipal() {
        controller = new ChatMessageController(chatMessageCommandUseCase, chatTypingUseCase);

        assertThatThrownBy(() -> controller.sendMessage(45L, new SendMessageRequest("내용"), null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("BusinessException은 에러 코드와 메시지를 담은 ChatErrorMessage로 변환된다")
    void handleBusinessException_convertsToChatErrorMessage() {
        controller = new ChatMessageController(chatMessageCommandUseCase, chatTypingUseCase);

        ChatErrorMessage result = controller.handleBusinessException(new BusinessException(ErrorCode.CHAT_FORBIDDEN));

        assertThat(result.errorCode()).isEqualTo(ErrorCode.CHAT_FORBIDDEN.getCode());
        assertThat(result.message()).isEqualTo(ErrorCode.CHAT_FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("BusinessException 외의 예외(예: 인증 정보 없음)도 에러 프레임으로 변환된다")
    void handleUnexpectedException_convertsToChatErrorMessage() {
        controller = new ChatMessageController(chatMessageCommandUseCase, chatTypingUseCase);

        ChatErrorMessage result = controller.handleUnexpectedException(new IllegalStateException("STOMP 세션에 인증 정보가 없습니다."));

        assertThat(result.errorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getCode());
    }
}
