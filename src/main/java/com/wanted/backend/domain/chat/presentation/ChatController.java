package com.wanted.backend.domain.chat.presentation;

import com.wanted.backend.domain.chat.application.result.SocketTicketResult;
import com.wanted.backend.domain.chat.application.usecase.SocketTicketCommandUseCase;
import com.wanted.backend.domain.chat.presentation.response.SocketTicketResponse;
import com.wanted.backend.global.common.ApiResponse;
import com.wanted.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final SocketTicketCommandUseCase socketTicketCommandUseCase;

    public ChatController(SocketTicketCommandUseCase socketTicketCommandUseCase) {
        this.socketTicketCommandUseCase = socketTicketCommandUseCase;
    }

    @Operation(
            summary = "소켓 티켓 발급",
            description = """
                STOMP CONNECT 시 사용할 단기 1회용 인증 티켓을 발급합니다.
                - 발급 즉시 Redis에 30초 TTL로 저장되며, WebSocket 연결 시 1회 소비되면 재사용할 수 없습니다.
                """
    )
    @PostMapping("/socket-tickets")
    public ResponseEntity<ApiResponse<SocketTicketResponse>> issueSocketTicket(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        SocketTicketResult result = socketTicketCommandUseCase.issue(userDetails.getMemberId());

        return ApiResponse.created("소켓 티켓이 발급되었습니다.", SocketTicketResponse.from(result));
    }
}
