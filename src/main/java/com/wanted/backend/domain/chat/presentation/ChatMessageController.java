package com.wanted.backend.domain.chat.presentation;

import com.wanted.backend.domain.chat.application.command.SendMessageCommand;
import com.wanted.backend.domain.chat.application.usecase.ChatMessageCommandUseCase;
import com.wanted.backend.domain.chat.application.usecase.ChatTypingUseCase;
import com.wanted.backend.domain.chat.infrastructure.websocket.ChatPrincipal;
import com.wanted.backend.domain.chat.presentation.request.SendMessageRequest;
import com.wanted.backend.domain.chat.presentation.response.ChatErrorMessage;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatMessageController {

    private final ChatMessageCommandUseCase chatMessageCommandUseCase;
    private final ChatTypingUseCase chatTypingUseCase;

    public ChatMessageController(ChatMessageCommandUseCase chatMessageCommandUseCase,
                                 ChatTypingUseCase chatTypingUseCase) {
        this.chatMessageCommandUseCase = chatMessageCommandUseCase;
        this.chatTypingUseCase = chatTypingUseCase;
    }

    @MessageMapping("/chat-rooms/{chatRoomId}")
    public void sendMessage(@DestinationVariable Long chatRoomId,
                            @Payload SendMessageRequest request,
                            Principal principal) {

        chatMessageCommandUseCase.send(new SendMessageCommand(
                chatRoomId, extractMemberId(principal), request.content()));
    }

    @MessageMapping("/chat-rooms/{chatRoomId}/typing")
    public void notifyTyping(@DestinationVariable Long chatRoomId, Principal principal) {
        chatTypingUseCase.notifyTyping(chatRoomId, extractMemberId(principal));
    }

    @MessageExceptionHandler(BusinessException.class)
    @SendToUser("/queue/errors")
    public ChatErrorMessage handleBusinessException(BusinessException e) {
        return new ChatErrorMessage(e.getErrorCode().getCode(), e.getMessage());
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public ChatErrorMessage handleUnexpectedException(Exception e) {
        return new ChatErrorMessage(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
    }

    private Long extractMemberId(Principal principal) {
        if (principal instanceof ChatPrincipal chatPrincipal) {
            return chatPrincipal.getMemberId();
        }
        throw new IllegalStateException("STOMP 세션에 인증 정보가 없습니다.");
    }
}
