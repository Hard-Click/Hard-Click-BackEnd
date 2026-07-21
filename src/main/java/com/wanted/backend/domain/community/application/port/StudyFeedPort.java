package com.wanted.backend.domain.community.application.port;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 전체 피드에 합칠 스터디 모집글 조회 포트.
 *
 * <p>해산(DISSOLVED)된 스터디는 제외한 활성 모집글만 반환한다(스터디 탭과 동일 기준).
 */
public interface StudyFeedPort {

    List<StudyFeedItem> findActiveStudies();

    record StudyFeedItem(
            Long groupId,
            String title,
            String authorName,
            String subjectName,
            int currentCount,
            int maxCount,
            boolean isClosed,
            LocalDateTime createdAt
    ) {}
}
