package com.wanted.backend.domain.payment.presentation;

import com.wanted.backend.domain.order.application.usecase.AdminRefundOrderUseCase;
import com.wanted.backend.domain.payment.application.port.AdminPaymentQueryPort;
import com.wanted.backend.domain.payment.application.usecase.GetAdminPaymentsUseCase;
import com.wanted.backend.domain.payment.domain.model.PaymentStatus;
import com.wanted.backend.domain.payment.presentation.response.AdminPaymentListResponse;
import com.wanted.backend.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
@Tag(name = "Admin Payment", description = "관리자 결제 관리 API")
public class AdminPaymentController {

    private final GetAdminPaymentsUseCase getAdminPaymentsUseCase;
    private final AdminRefundOrderUseCase adminRefundOrderUseCase;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "관리자 결제 목록 조회",
            description = "주문번호, 사용자명, 이메일로 검색하고 결제 상태로 필터링합니다. ADMIN 권한 필요."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "관리자 결제 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 없음")
    })
    public ResponseEntity<ApiResponse<AdminPaymentListResponse>> getPayments(
            @Parameter(description = "결제 상태 필터 (미지정 시 전체)", example = "PAID")
            @RequestParam(required = false) PaymentStatus status,
            @Parameter(description = "주문번호/사용자명/이메일 검색어")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "페이지 번호 (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 항목 수", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        // paidAt 단독 정렬은 동률(같은 결제 시각) 시 페이지 경계에서 순서가 불안정해 행 누락/중복이
        // 생길 수 있어, 결정적 보조 키(id DESC)를 더해 안정 정렬을 보장한다.
        Sort sort = Sort.by(Sort.Direction.DESC, "paidAt").and(Sort.by(Sort.Direction.DESC, "id"));
        Page<AdminPaymentQueryPort.AdminPaymentData> result = getAdminPaymentsUseCase.handle(
                status, keyword, PageRequest.of(page, size, sort));
        return ApiResponse.success("관리자 결제 목록 조회 성공", AdminPaymentListResponse.from(result));
    }

    @PostMapping("/{paymentId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "결제 환불 처리",
            description = "결제완료 상태의 결제를 환불 처리합니다. 강의 결제인 경우 수강 권한이 즉시 박탈되며 복구할 수 없습니다. ADMIN 권한 필요."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "환불 처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "결제 정보를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 환불된 결제이거나 환불 불가 상태")
    })
    public ResponseEntity<ApiResponse<Void>> refund(
            @Parameter(description = "환불할 결제 ID (= 주문 ID, 목록의 paymentId)", example = "203")
            @PathVariable Long paymentId) {
        adminRefundOrderUseCase.refund(paymentId);
        return ApiResponse.successNoContent("환불이 처리되었습니다.");
    }
}
