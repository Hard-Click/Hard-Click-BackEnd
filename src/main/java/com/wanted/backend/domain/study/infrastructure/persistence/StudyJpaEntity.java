package com.wanted.backend.domain.study.infrastructure.persistence;

import com.wanted.backend.domain.study.domain.model.StudyStatus;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(name = "study", indexes = {
        @Index(name = "idx_study_status_created", columnList = "status, created_at")
})
@Getter
public class StudyJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "study_id")
    private Long id;

    @Column(name = "host_id", nullable = false)
    private Long hostId;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, length = 50)
    private String subject;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "max_count", nullable = false)
    private int maxCount;

    @Column(name = "current_count", nullable = false)
    private int currentCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StudyStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected StudyJpaEntity() {}

    public StudyJpaEntity(Long hostId, String title, String subject, String content,
                          int maxCount, int currentCount, StudyStatus status,
                          LocalDateTime createdAt, LocalDateTime updatedAt) {
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
}
