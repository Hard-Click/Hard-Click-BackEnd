package com.wanted.backend.domain.notice.application.service;

import com.wanted.backend.domain.notice.application.command.CreateGlobalNoticeCommand;
import com.wanted.backend.domain.notice.application.command.CreateNoticeCommand;
import com.wanted.backend.domain.notice.application.command.DeleteNoticeCommand;
import com.wanted.backend.domain.notice.application.command.UpdateNoticeCommand;
import com.wanted.backend.domain.notice.application.policy.GlobalNoticeCreatePolicy;
import com.wanted.backend.domain.notice.application.policy.NoticeCreatePolicy;
import com.wanted.backend.domain.notice.application.policy.NoticeUpdatePolicy;
import com.wanted.backend.domain.notice.application.port.AdminValidationPort;
import com.wanted.backend.domain.notice.application.port.NoticeReadPort;
import com.wanted.backend.domain.notice.application.usecase.NoticeCommandUseCase;
import com.wanted.backend.domain.notice.domain.event.NoticeChangedEvent;
import com.wanted.backend.domain.notice.domain.event.NoticeCreatedEvent;
import com.wanted.backend.domain.notice.domain.model.Notice;
import com.wanted.backend.domain.notice.domain.repository.NoticeRepository;
import com.wanted.backend.domain.notification.domain.repository.NotificationRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NoticeCommandService implements NoticeCommandUseCase {

    private final NoticeRepository noticeRepository;
    private final NoticeCreatePolicy noticeCreatePolicy;
    private final GlobalNoticeCreatePolicy globalNoticeCreatePolicy;
    private final NoticeUpdatePolicy noticeUpdatePolicy;
    private final AdminValidationPort adminValidationPort;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationRepository notificationRepository;
    private final NoticeReadPort noticeReadPort;

    public NoticeCommandService(NoticeRepository noticeRepository,
                                NoticeCreatePolicy noticeCreatePolicy,
                                GlobalNoticeCreatePolicy globalNoticeCreatePolicy,
                                NoticeUpdatePolicy noticeUpdatePolicy,
                                AdminValidationPort adminValidationPort,
                                ApplicationEventPublisher eventPublisher,
                                NotificationRepository notificationRepository,
                                NoticeReadPort noticeReadPort) {
        this.noticeRepository = noticeRepository;
        this.noticeCreatePolicy = noticeCreatePolicy;
        this.globalNoticeCreatePolicy = globalNoticeCreatePolicy;
        this.noticeUpdatePolicy = noticeUpdatePolicy;
        this.adminValidationPort = adminValidationPort;
        this.eventPublisher = eventPublisher;
        this.notificationRepository = notificationRepository;
        this.noticeReadPort = noticeReadPort;
    }

    @Override
    public Long create(CreateNoticeCommand command) {
        noticeCreatePolicy.validate(command.instructorId(), command.courseId());

        Notice notice = Notice.create(command.instructorId(), command.courseId(),
                command.title(), command.content(), command.isPinned());
        Notice saved = noticeRepository.save(notice);

        boolean isAdmin = adminValidationPort.isAdmin(command.instructorId());
        eventPublisher.publishEvent(NoticeCreatedEvent.of(
                saved.getId(), saved.getCourseId(), "COURSE", saved.getTitle(), isAdmin));

        return saved.getId();
    }

    // 전체 공지 변경은 관리자 대시보드의 recentNotices/totalNoticeCount(GLOBAL·PUBLISHED 기준)에
    // 영향을 준다. 캐시 무효화는 NoticeChangedEvent를 발행해 admin_dashboard 리스너가 커밋 후
    // 처리한다(커밋 전 stale 재적재 방지 + evict 실패를 공지 작업과 격리). @CacheEvict를 직접 걸면
    // 트랜잭션 커밋과 분리돼 위 두 문제를 못 막는다.
    @Override
    public Long createGlobal(CreateGlobalNoticeCommand command) {
        globalNoticeCreatePolicy.validate(command.adminId());

        Notice notice = Notice.createGlobal(command.adminId(),
                command.title(), command.content(), command.isPinned());
        Notice saved = noticeRepository.save(notice);

        eventPublisher.publishEvent(NoticeCreatedEvent.of(
                saved.getId(), null, "GLOBAL", saved.getTitle(), true));
        eventPublisher.publishEvent(NoticeChangedEvent.of(
                saved.getId(), NoticeChangedEvent.ChangeType.CREATED_GLOBAL));

        return saved.getId();
    }

    // 수정/삭제는 GLOBAL/COURSE 모두 대상일 수 있으나, 대시보드 캐시는 단일 'summary' 키라
    // 항상 무효화해도 부담이 없고 최신성이 보장된다.
    @Override
    public void update(UpdateNoticeCommand command) {
        Notice notice = noticeRepository.findById(command.noticeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_FOUND));
        noticeUpdatePolicy.validate(command.memberId(), notice);
        notice.update(command.title(), command.content(), command.isPinned());
        noticeRepository.save(notice);

        eventPublisher.publishEvent(NoticeChangedEvent.of(
                notice.getId(), NoticeChangedEvent.ChangeType.UPDATED));
    }

    @Override
    public void delete(DeleteNoticeCommand command) {
        Notice notice = noticeRepository.findById(command.noticeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_FOUND));
        noticeUpdatePolicy.validate(command.memberId(), notice);
        noticeRepository.deleteById(command.noticeId());
        notificationRepository.deleteByRedirectUrlStartingWith("/notices/" + command.noticeId());

        eventPublisher.publishEvent(NoticeChangedEvent.of(
                command.noticeId(), NoticeChangedEvent.ChangeType.DELETED));
    }

    @Override
    public void markAsRead(Long memberId, Long noticeId) {
        // 존재하지 않는 공지는 읽음 처리 대상이 아니다. 조회/목록과 동일하게 공개 리소스라 소유 검증은 없다.
        if (noticeRepository.findById(noticeId).isEmpty()) {
            throw new BusinessException(ErrorCode.NOTICE_NOT_FOUND);
        }
        noticeReadPort.markRead(memberId, noticeId);
    }
}