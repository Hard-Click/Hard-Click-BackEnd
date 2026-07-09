package com.wanted.backend.domain.chat.presentation;

import com.wanted.backend.domain.chat.application.result.ChatRoomDetailResult;
import com.wanted.backend.domain.chat.application.usecase.ChatRoomQueryUseCase;
import com.wanted.backend.domain.chat.presentation.response.ChatRoomResponse;
import com.wanted.backend.global.common.ApiResponse;
import com.wanted.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat/rooms")
public class ChatRoomController {

    private final ChatRoomQueryUseCase chatRoomQueryUseCase;

    public ChatRoomController(ChatRoomQueryUseCase chatRoomQueryUseCase) {
        this.chatRoomQueryUseCase = chatRoomQueryUseCase;
    }

    @Operation(
            summary = "채팅방 정보 조회",
            description = """
                채팅방 상세 정보를 조회합니다.
                - 채팅방 참여자만 조회할 수 있습니다.
                - 스터디 제목/과목명/방장 정보와 참여자별 접속(presence) 여부를 함께 반환합니다.
                """
    )
    @GetMapping("/{chatRoomId}")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> getRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long chatRoomId) {

        ChatRoomDetailResult result = chatRoomQueryUseCase.getRoom(chatRoomId, userDetails.getMemberId());

        return ApiResponse.success("채팅방 조회 성공", ChatRoomResponse.from(result));
    }
}
