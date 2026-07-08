package com.wanted.backend.domain.study.domain.model;

import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;

import java.time.LocalDateTime;

public class Study {

    private Long id;
    private Long hostId;
    private String title;
    private String subject;
    private String content;
    private int maxCount;
    private int currentCount;
    private StudyStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Study(Long id, Long hostId, String title, String subject, String content,
                  int maxCount, int currentCount, StudyStatus status,
                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.hostId = hostId;
        this.title = title;
        this.subject = subject;
        this.content = content;
        this.maxCount = maxCount;
        this.currentCount = currentCount;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Study create(Long hostId, String title, String subject, int maxCount, String content) {
        if (maxCount < 2) {
            throw new BusinessException(ErrorCode.STUDY_MIN_COUNT_INVALID);
        }
        LocalDateTime now = LocalDateTime.now();
        return new Study(null, hostId, title, subject, content, maxCount, 1, StudyStatus.ACTIVE, now, now);
    }

    public static Study restore(Long id, Long hostId, String title, String subject, String content,
                                int maxCount, int currentCount, StudyStatus status,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Study(id, hostId, title, subject, content, maxCount, currentCount, status, createdAt, updatedAt);
    }

    public boolean isOwner(Long memberId) {
        return this.hostId.equals(memberId);
    }

    public void update(Long requesterId, String title, String subject, int maxCount, String content) {
        validateUpdatable(requesterId);
        if (maxCount < 2) {
            throw new BusinessException(ErrorCode.STUDY_MIN_COUNT_INVALID);
        }
        if (maxCount < currentCount) {
            throw new BusinessException(ErrorCode.STUDY_MAX_COUNT_BELOW_CURRENT);
        }
        this.title = title;
        this.subject = subject;
        this.maxCount = maxCount;
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    private void validateUpdatable(Long memberId) {
        if (!isOwner(memberId)) {
            throw new BusinessException(ErrorCode.STUDY_UPDATE_FORBIDDEN);
        }
    }

    public Long getId() { return id; }
    public Long getHostId() { return hostId; }
    public String getTitle() { return title; }
    public String getSubject() { return subject; }
    public String getContent() { return content; }
    public int getMaxCount() { return maxCount; }
    public int getCurrentCount() { return currentCount; }
    public StudyStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
