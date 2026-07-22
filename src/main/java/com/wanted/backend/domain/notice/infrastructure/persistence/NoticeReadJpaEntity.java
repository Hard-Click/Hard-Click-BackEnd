package com.wanted.backend.domain.notice.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * notice_read 매핑 엔티티. (member_id, notice_id) 유니크로 1회원-1공지 1행을 보장한다.
 */
@Entity
@Table(name = "notice_read",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notice_read_member_notice",
                columnNames = {"member_id", "notice_id"}))
@Getter
public class NoticeReadJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "notice_id", nullable = false)
    private Long noticeId;

    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt;

    protected NoticeReadJpaEntity() {
    }

    public NoticeReadJpaEntity(Long memberId, Long noticeId, LocalDateTime readAt) {
        this.memberId = memberId;
        this.noticeId = noticeId;
        this.readAt = readAt;
    }
}
