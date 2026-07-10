package com.wanted.backend.domain.chat.application.usecase;

import com.wanted.backend.domain.chat.application.result.ChatRoomDetailResult;
import com.wanted.backend.domain.chat.application.result.MyChatRoomDetail;

import java.util.List;

public interface ChatRoomQueryUseCase {
    ChatRoomDetailResult getRoom(Long chatRoomId, Long memberId);

    List<MyChatRoomDetail> getMyRooms(Long memberId);
}
