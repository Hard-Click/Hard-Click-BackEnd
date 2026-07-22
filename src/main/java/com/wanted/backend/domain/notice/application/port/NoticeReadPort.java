package com.wanted.backend.domain.notice.application.port;

import java.util.List;

/**
 * 공지 읽음 상태(notice_read) 아웃바운드 포트.
 * 알림(notification) 존재 여부와 무관하게 (회원, 공지) 단위로 읽음을 관리한다.
 */
public interface NoticeReadPort {

    /** 읽음 처리. 이미 읽음이면 아무 일도 하지 않는다(멱등). */
    void markRead(Long memberId, Long noticeId);

    /** 단건 읽음 여부. memberId가 null(비로그인)이면 항상 false. */
    boolean isRead(Long memberId, Long noticeId);

    /** noticeIds 중 해당 회원이 읽은 공지 ID 목록. memberId가 null이면 빈 목록. */
    List<Long> findReadNoticeIds(Long memberId, List<Long> noticeIds);
}
