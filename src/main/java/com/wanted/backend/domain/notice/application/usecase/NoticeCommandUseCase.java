package com.wanted.backend.domain.notice.application.usecase;

import com.wanted.backend.domain.notice.application.command.CreateGlobalNoticeCommand;
import com.wanted.backend.domain.notice.application.command.CreateNoticeCommand;
import com.wanted.backend.domain.notice.application.command.DeleteNoticeCommand;
import com.wanted.backend.domain.notice.application.command.UpdateNoticeCommand;


public interface NoticeCommandUseCase {
    Long create(CreateNoticeCommand command);
    Long createGlobal(CreateGlobalNoticeCommand command);
    void update(UpdateNoticeCommand command);
    void delete(DeleteNoticeCommand command);

    /** 공지 읽음 처리(회원 기준). 이미 읽음이면 멱등하게 통과. */
    void markAsRead(Long memberId, Long noticeId);
}