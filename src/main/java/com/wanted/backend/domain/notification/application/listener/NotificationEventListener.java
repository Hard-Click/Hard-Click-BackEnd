package com.wanted.backend.domain.notification.application.listener;

import com.wanted.backend.domain.community.domain.event.CommentAcceptedEvent;
import com.wanted.backend.domain.community.domain.event.CommentReplyCreatedEvent;
import com.wanted.backend.domain.community.domain.event.PostCommentCreatedEvent;
import com.wanted.backend.domain.community.domain.event.ReportCreatedEvent;
import com.wanted.backend.domain.cource.domain.event.CourseCreatedEvent;
import com.wanted.backend.domain.notice.domain.event.NoticeCreatedEvent;
import com.wanted.backend.domain.notification.application.dto.NotificationRequest;
import com.wanted.backend.domain.notification.application.port.CourseEnrolleeQueryPort;
import com.wanted.backend.domain.notification.application.port.MemberIdQueryPort;
import com.wanted.backend.domain.notification.application.usecase.NotificationCommandUseCase;
import com.wanted.backend.domain.notification.domain.model.NotificationType;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class NotificationEventListener {

    private final NotificationCommandUseCase notificationCommandUseCase;
    private final CourseEnrolleeQueryPort courseEnrolleeQueryPort;
    private final MemberIdQueryPort memberIdQueryPort;
    private final MeterRegistry meterRegistry;

    public NotificationEventListener(NotificationCommandUseCase notificationCommandUseCase,
                                     CourseEnrolleeQueryPort courseEnrolleeQueryPort,
                                     MemberIdQueryPort memberIdQueryPort, MeterRegistry meterRegistry) {
        this.notificationCommandUseCase = notificationCommandUseCase;
        this.courseEnrolleeQueryPort = courseEnrolleeQueryPort;
        this.memberIdQueryPort = memberIdQueryPort;
        this.meterRegistry = meterRegistry;
    }

    // ── 커뮤니티 ──────────────────────────────────────────────────

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostComment(PostCommentCreatedEvent event) {
        if (event.postAuthorId().equals(event.commentAuthorId())) return;
        try {
            notificationCommandUseCase.send(
                    event.postAuthorId(),
                    NotificationType.POST_COMMENT,
                    "내 게시글에 새 댓글이 달렸습니다.",
                    "/community/" + event.postId()
            );
        } catch (Exception e) {
            log.error("[Notification] onPostComment 처리 실패. postId={}", event.postId(), e);
        }
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentReply(CommentReplyCreatedEvent event) {
        if (event.parentCommentAuthorId().equals(event.replyAuthorId())) return;
        try {
            notificationCommandUseCase.send(
                    event.parentCommentAuthorId(),
                    NotificationType.COMMENT_REPLY,
                    "내 댓글에 대댓글이 달렸습니다.",
                    "/community/" + event.postId()
            );
        } catch (Exception e) {
            log.error("[Notification] onCommentReply 처리 실패. postId={}", event.postId(), e);
        }
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentAccepted(CommentAcceptedEvent event) {
        try {
            notificationCommandUseCase.send(
                    event.commentAuthorId(),
                    NotificationType.COMMENT_ACCEPTED,
                    "내 댓글이 채택되었습니다.",
                    "/community/" + event.postId()
            );
        } catch (Exception e) {
            log.error("[Notification] onCommentAccepted 처리 실패. commentId={}", event.commentId(), e);
        }
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReportCreated(ReportCreatedEvent event) {
        try {
            List<NotificationRequest> requests = memberIdQueryPort.findAllAdminIds().stream()
                    .map(adminId -> new NotificationRequest(
                            adminId,
                            NotificationType.REPORT,
                            "새로운 신고가 접수되었습니다.",
                            "/admin/reports/" + event.reportId()
                    ))
                    .toList();
            notificationCommandUseCase.sendBatch(requests);
        } catch (Exception e) {
            log.error("[Notification] onReportCreated 처리 실패. reportId={}", event.reportId(), e);
        }
    }

    // ── 공지 ──────────────────────────────────────────────────────

    // 110~146줄 교체
    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNoticeCreated(NoticeCreatedEvent event) {
        try {
            String url = "/notices/" + event.noticeId();

            if ("GLOBAL".equals(event.type())) {
                String message = "새로운 전체 공지가 등록되었습니다: " + event.title();
                List<NotificationRequest> requests = new ArrayList<>();

                Timer.Sample memberQuerySample = Timer.start(meterRegistry);
                memberIdQueryPort.findAllInstructorIds().forEach(id ->
                        requests.add(new NotificationRequest(id, NotificationType.NOTICE, message, url)));
                memberIdQueryPort.findAllStudentIds().forEach(id ->
                        requests.add(new NotificationRequest(id, NotificationType.NOTICE, message, url)));
                memberQuerySample.stop(Timer.builder("notification.member.query")
                        .tag("type", "GLOBAL")
                        .publishPercentileHistogram(true)
                        .register(meterRegistry));

                notificationCommandUseCase.sendBatch(requests);

            } else {
                List<NotificationRequest> requests = new ArrayList<>();

                if (!event.createdByAdmin()) {
                    memberIdQueryPort.findAllAdminIds().forEach(adminId ->
                            requests.add(new NotificationRequest(
                                    adminId,
                                    NotificationType.NOTICE,
                                    "강사가 강의 공지를 등록했습니다: " + event.title(),
                                    url
                            )));
                }

                Timer.Sample memberQuerySample = Timer.start(meterRegistry);
                courseEnrolleeQueryPort.findMemberIdsByCourseId(event.courseId()).forEach(memberId ->
                        requests.add(new NotificationRequest(
                                memberId, NotificationType.NOTICE,
                                "수강 중인 강의에 새 공지가 등록되었습니다: " + event.title(), url)));
                memberQuerySample.stop(Timer.builder("notification.member.query")
                        .tag("type", "COURSE")
                        .register(meterRegistry));

                notificationCommandUseCase.sendBatch(requests);
            }
        } catch (Exception e) {
            log.error("[Notification] onNoticeCreated 처리 실패. noticeId={}", event.noticeId(), e);
        }
    }

    // ── 강좌 개설 ─────────────────────────────────────────────────

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCourseCreated(CourseCreatedEvent event) {
        try {
            List<NotificationRequest> requests = memberIdQueryPort.findAllAdminIds().stream()
                    .map(adminId -> new NotificationRequest(
                            adminId,
                            NotificationType.COURSE_REGISTER,
                            "새로운 강좌가 등록되었습니다: " + event.title(),
                            "/admin/courses/manage/" + event.courseId()
                    ))
                    .toList();
            notificationCommandUseCase.sendBatch(requests);
        } catch (Exception e) {
            log.error("[Notification] onCourseCreated 처리 실패. courseId={}", event.courseId(), e);
        }
    }
}