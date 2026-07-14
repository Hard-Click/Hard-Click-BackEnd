package com.wanted.backend.domain.payment.application.service;

import com.wanted.backend.domain.payment.application.port.MyPaymentHistoryQueryPort;
import com.wanted.backend.domain.payment.application.port.PaymentCourseDisplayNamePort;
import com.wanted.backend.domain.payment.application.port.PaymentSubscriptionPlanDisplayNamePort;
import com.wanted.backend.domain.payment.application.usecase.GetMyPaymentHistoryUseCase;
import com.wanted.backend.domain.payment.domain.model.PaymentType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPaymentHistoryService implements GetMyPaymentHistoryUseCase {

    private static final int MAX_PAGE_SIZE = 50;

    private final MyPaymentHistoryQueryPort myPaymentHistoryQueryPort;
    private final PaymentCourseDisplayNamePort paymentCourseDisplayNamePort;
    private final PaymentSubscriptionPlanDisplayNamePort paymentSubscriptionPlanDisplayNamePort;

    @Override
    public Page<MyPaymentHistoryView> handle(Long memberId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(
                Math.max(page, 0),
                normalizeSize(size),
                Sort.by(Sort.Direction.DESC, "paidAt", "id")
        );
        Page<MyPaymentHistoryQueryPort.MyPaymentHistoryData> histories =
                myPaymentHistoryQueryPort.findByMemberId(memberId, pageRequest);

        Map<Long, String> courseNameById = paymentCourseDisplayNamePort.findNamesByCourseIds(
                histories.getContent().stream()
                        .flatMap(history -> history.courseIds().stream())
                        .distinct()
                        .toList()
        );
        Map<Long, String> planNameById = paymentSubscriptionPlanDisplayNamePort.findNamesByPlanIds(
                histories.getContent().stream()
                        .map(MyPaymentHistoryQueryPort.MyPaymentHistoryData::subscriptionPlanId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList()
        );

        return histories.map(history -> toView(history, courseNameById, planNameById));
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return 10;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    // 결제/주문 조회 데이터와 별도 포트로 조회한 표시명을 합쳐 응답용 View로 변환한다.
    private MyPaymentHistoryView toView(
            MyPaymentHistoryQueryPort.MyPaymentHistoryData data,
            Map<Long, String> courseNameById,
            Map<Long, String> planNameById
    ) {
        return new MyPaymentHistoryView(
                data.paymentId(),
                data.orderId(),
                data.orderNo(),
                data.paymentType(),
                data.amount(),
                data.status(),
                data.paidAt(),
                displayName(data, courseNameById, planNameById)
        );
    }

    private String displayName(
            MyPaymentHistoryQueryPort.MyPaymentHistoryData data,
            Map<Long, String> courseNameById,
            Map<Long, String> planNameById
    ) {
        if (Objects.equals(data.paymentType(), PaymentType.SUBSCRIPTION)) {
            Long planId = data.subscriptionPlanId();
            // 결제는 됐으나 구독이 생성되지 않은 주문(과거 subscription_id auto_increment 버그로 지급 실패)은
            // planId가 null이다. planNameById가 불변 Map(Map.of())일 때 getOrDefault(null, ...)은
            // NPE를 던지므로(불변 Map은 null 키 조회 거부) 반드시 null을 먼저 방어한다.
            if (planId == null) {
                return "구독권";
            }
            return planNameById.getOrDefault(planId, "(삭제된 구독 플랜)");
        }

        if (data.courseIds().isEmpty()) {
            return "(삭제된 강의)";
        }

        return data.courseIds().stream()
                .map(courseId -> courseNameById.getOrDefault(courseId, "(삭제된 강의)"))
                .distinct()
                .reduce((first, second) -> first + ", " + second)
                .orElse("(삭제된 강의)");
    }
}
