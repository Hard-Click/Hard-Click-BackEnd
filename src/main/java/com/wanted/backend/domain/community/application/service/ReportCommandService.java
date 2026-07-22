package com.wanted.backend.domain.community.application.service;

import com.wanted.backend.domain.community.application.command.CreateReportCommand;
import com.wanted.backend.domain.community.application.policy.CommunityAccessPolicy;
import com.wanted.backend.domain.community.application.port.MemberAutoSuspendPort;
import com.wanted.backend.domain.community.application.usecase.ReportCommandUseCase;
import com.wanted.backend.domain.community.domain.event.ReportCreatedEvent;
import com.wanted.backend.domain.community.domain.model.Report;
import com.wanted.backend.domain.community.domain.model.TargetType;
import com.wanted.backend.domain.community.domain.repository.CommentRepository;
import com.wanted.backend.domain.community.domain.repository.PostRepository;
import com.wanted.backend.domain.community.domain.repository.ReportRepository;
import com.wanted.backend.domain.community.domain.repository.ReviewRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class ReportCommandService implements ReportCommandUseCase {

    private static final int REPORT_FLAG_THRESHOLD = 3;
    private static final int AUTO_SUSPEND_REPORT_THRESHOLD = 50;

    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ReviewRepository reviewRepository;
    private final CommunityAccessPolicy communityAccessPolicy;
    private final ApplicationEventPublisher eventPublisher;
    private final MemberAutoSuspendPort memberAutoSuspendPort;

    public ReportCommandService(ReportRepository reportRepository,
                                PostRepository postRepository,
                                CommentRepository commentRepository,
                                ReviewRepository reviewRepository,
                                CommunityAccessPolicy communityAccessPolicy,
                                ApplicationEventPublisher eventPublisher,
                                MemberAutoSuspendPort memberAutoSuspendPort) {
        this.reportRepository = reportRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.reviewRepository = reviewRepository;
        this.communityAccessPolicy = communityAccessPolicy;
        this.eventPublisher = eventPublisher;
        this.memberAutoSuspendPort = memberAutoSuspendPort;
    }

    @Override
    public Long create(CreateReportCommand command) {
        communityAccessPolicy.validateAccess(command.reporterId());

        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(
                command.reporterId(), command.targetType(), command.targetId())) {
            throw new BusinessException(ErrorCode.REPORT_ALREADY_EXISTS);
        }

        Long reportedMemberId = findReportedMemberId(command.targetType(), command.targetId());

        Long reportId = reportRepository.save(Report.create(
                command.reporterId(),
                reportedMemberId,
                command.targetType(),
                command.targetId(),
                command.reportTypes(),
                command.reason()
        ));

        int count = reportRepository.countByTargetTypeAndTargetId(
                command.targetType(), command.targetId());
        if (count >= REPORT_FLAG_THRESHOLD) {
            log.warn("[Report Flag] targetType: {}, targetId: {}, count: {}",
                    command.targetType(), command.targetId(), count);
            // 관리자 신고 목록은 3건 이상 누적된 대상만 노출한다(AdminReportQueryAdapter).
            // 1·2건째에 알림만 먼저 가면 목록엔 아무것도 안 보여 혼란스러우므로,
            // 알림도 동일 기준(3건째부터)으로 발송한다. 4·5건째 이후는 매번 발송된다.
            eventPublisher.publishEvent(ReportCreatedEvent.of(
                    reportId, command.targetType().name(), command.targetId()));
        }
        // 회원 단위 자동 차단 로직
        int totalReportCount = reportRepository.countByReportedMemberId(reportedMemberId);
        if (totalReportCount >= AUTO_SUSPEND_REPORT_THRESHOLD) {
            memberAutoSuspendPort.suspendForReportThreshold(reportedMemberId);
        }

        return reportId;
    }

    private Long findReportedMemberId(TargetType targetType, Long targetId) {
        return switch (targetType) {
            case POST -> postRepository.findById(targetId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_TARGET_NOT_FOUND))
                    .getAuthorId();
            case COMMENT -> commentRepository.findById(targetId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_TARGET_NOT_FOUND))
                    .getAuthorId();
            case REVIEW -> reviewRepository.findById(targetId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_TARGET_NOT_FOUND))
                    .getMemberId();
        };
    }
}