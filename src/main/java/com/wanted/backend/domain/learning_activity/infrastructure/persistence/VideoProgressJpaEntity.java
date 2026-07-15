package com.wanted.backend.domain.learning_activity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "video_progress",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_video_progress_member_video",
                columnNames = {"member_id", "video_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VideoProgressJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "progress_id")
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "video_id", nullable = false)
    private Long videoId;

    @Column(name = "last_position_sec", nullable = false)
    private Integer lastPositionSec;

    @Column(name = "watch_time_sec", nullable = false)
    private Integer watchTimeSec;

    @Column(name = "is_completed", nullable = false)
    private Boolean completed;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public VideoProgressJpaEntity(
            Long memberId,
            Long courseId,
            Long videoId,
            Integer lastPositionSec,
            Integer watchTimeSec,
            Boolean completed,
            LocalDateTime completedAt,
            LocalDateTime updatedAt
    ) {
        this.memberId = memberId;
        this.courseId = courseId;
        this.videoId = videoId;
        this.lastPositionSec = lastPositionSec;
        this.watchTimeSec = watchTimeSec;
        this.completed = completed;
        this.completedAt = completedAt;
        this.updatedAt = updatedAt;
    }

    public void updateLastPosition(Integer lastPositionSec, LocalDateTime updatedAt) {
        this.lastPositionSec = lastPositionSec;
        this.updatedAt = updatedAt;
    }

    public void updateProgress(
            Integer lastPositionSec,
            Integer watchTimeSec,
            Boolean completed,
            LocalDateTime completedAt,
            LocalDateTime updatedAt
    ) {
        this.lastPositionSec = lastPositionSec;
        this.watchTimeSec = watchTimeSec;
        this.completed = completed;
        this.completedAt = completedAt;
        this.updatedAt = updatedAt;
    }
}
