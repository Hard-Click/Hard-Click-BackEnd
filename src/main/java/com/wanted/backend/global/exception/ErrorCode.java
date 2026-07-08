package com.wanted.backend.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

    /**
     * 프로젝트 전역에서 사용할 에러 코드 모음
     */
    @Getter
    public enum ErrorCode {

    /* =========================================================================
       1. 공통 예외 (C000)
       ========================================================================= */
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "입력값이 올바르지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 내부 오류가 발생했습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C003", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "C004", "접근 권한이 없습니다."),

    /* =========================================================================
       2. 회원 및 계정 관련 예외 (U000)
       ========================================================================= */
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "가입되지 않은 이메일입니다."), // (명세서 9번 문구 반영)
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U002", "이미 사용 중인 이메일입니다."),
    INVALID_LOGIN_INFO(HttpStatus.UNAUTHORIZED, "U003", "아이디 또는 비밀번호가 올바르지 않습니다."),
    ACCOUNT_LOCKED(HttpStatus.LOCKED, "U004", "비밀번호를 5회 연속 틀려 계정이 잠겼습니다."),
        ACCOUNT_NOT_LOCKED(HttpStatus.CONFLICT, "U017", "잠긴 계정이 아닙니다."),
    SUSPENDED_MEMBER(HttpStatus.FORBIDDEN, "U015", "정지된 계정입니다."),
    ACCOUNT_ALREADY_LOCKED(HttpStatus.FORBIDDEN, "U020", "이미 잠긴 계정입니다. 이메일 인증을 통해 잠금을 해제해주세요."),
    INSTRUCTOR_NOT_FOUND(HttpStatus.UNAUTHORIZED, "U005", "제공받은 계정 정보가 존재하지 않습니다."),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "U006", "이미 사용 중인 아이디입니다."),
    FORBIDDEN_USERNAME(HttpStatus.BAD_REQUEST, "U007", "사용할 수 없는 아이디입니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "U008", "비밀번호가 일치하지 않습니다."),
    VERIFICATION_CODE_INVALID(HttpStatus.BAD_REQUEST, "U009", "유효하지 않은 인증코드입니다. 다시 입력해주세요."),
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "U010", "이메일 전송에 실패했습니다. 다시 시도해주세요."),
    INVALID_EMAIL_DOMAIN(HttpStatus.BAD_REQUEST, "U012", "gmail.com 이메일만 사용할 수 있습니다."),
    PASSWORD_RESET_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "U011", "비밀번호 재발급은 하루 최대 3회까지만 가능합니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "U013", "이메일 인증이 완료되지 않았습니다."),
    INVALID_ROLE(HttpStatus.BAD_REQUEST, "U014", "유효하지 않은 권한입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "U018", "회원 정보를 찾을 수 없습니다."),
    INVALID_MEMBER_STATUS_CHANGE(HttpStatus.BAD_REQUEST, "U019", "변경할 수 없는 회원 상태입니다."),
    PASSWORD_CONFIRM_MISMATCH(HttpStatus.BAD_REQUEST, "U016", "새 비밀번호와 비밀번호 확인이 일치하지 않습니다."),

    /* =========================================================================
       3. 인증 및 보안 관련 고도화 예외 (AUTH000)
       ========================================================================= */
    VERIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH_002", "인증 요청 내역이 없습니다."),
    VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH_003", "인증번호가 올바르지 않습니다."),
    VERIFICATION_EXPIRED(HttpStatus.GONE, "AUTH_004", "인증번호가 만료되었습니다."),
    ALREADY_REGISTERED_EMAIL(HttpStatus.CONFLICT, "AUTH_005", "이미 가입된 이메일입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_006", "유효하지 않은 Refresh Token입니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.GONE, "AUTH_007", "Refresh Token이 만료되었습니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND , "AUTH_008", "저장된 Refresh Token이 없습니다."),
    INVALID_CURRENT_PASSWORD(HttpStatus.UNAUTHORIZED, "AUTH_009", "현재 비밀번호가 일치하지 않습니다."),
    ALREADY_WITHDRAWN_MEMBER(HttpStatus.BAD_REQUEST, "AUTH_010","이미 탈퇴한 회원입니다."),
    WITHDRAWN_MEMBER(HttpStatus.FORBIDDEN,"AUTH_011", "탈퇴한 회원입니다."),

    /* =========================================================================
       4. 강의(Course) 관련 예외 (CR000)
       ========================================================================= */
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "CR001", "존재하지 않는 강의입니다."),
    INVALID_COURSE_PRICE(HttpStatus.BAD_REQUEST, "CR002", "강의 가격 설정이 올바르지 않습니다."),
    LESSON_NOT_FOUND(HttpStatus.NOT_FOUND, "CR003", "존재하지 않는 회차입니다."),
    COURSE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CR004", "해당 강의에 대한 권한이 없습니다."),
    COURSE_SECTION_NOT_FOUND(HttpStatus.NOT_FOUND, "CR005", "존재하지 않는 섹션입니다."),

    /* =========================================================================
       4-1. 퀴즈(Quiz) 관련 예외 (QZ000)
       ========================================================================= */
    QUIZ_NOT_FOUND(HttpStatus.NOT_FOUND, "QZ001", "존재하지 않는 퀴즈입니다."),
    QUIZ_NOT_AUTHORIZED(HttpStatus.FORBIDDEN, "QZ002", "본인이 등록한 퀴즈만 수정/삭제할 수 있습니다."),
    QUIZ_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "QZ003", "이미 제출한 퀴즈입니다."),
    QUIZ_ENROLLMENT_REQUIRED(HttpStatus.FORBIDDEN, "QZ004", "수강 중인 강의의 퀴즈만 응시/조회할 수 있습니다."),
    QUIZ_SUBMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "QZ005", "제출한 퀴즈만 결과를 조회할 수 있습니다."),

    /* =========================================================================
       5. 리뷰 도메인 관련 예외 (R000)
       ========================================================================= */
    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "R001", "이미 리뷰를 작성한 강의입니다."),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "R002", "존재하지 않는 리뷰입니다."),
    REVIEW_NOT_AUTHORIZED(HttpStatus.FORBIDDEN, "R003", "본인의 리뷰만 수정/삭제할 수 있습니다."),
    NOT_ENROLLED(HttpStatus.FORBIDDEN, "R004", "수강 중인 강의만 리뷰를 작성할 수 있습니다."),

    /* =========================================================================
       6. 수강신청 및 학습활동 관련 예외 (EN/L000)
       ========================================================================= */
    ENROLLMENT_ALREADY_EXISTS(HttpStatus.CONFLICT, "EN001", "이미 수강 중인 강의입니다."),
    VIDEO_NOT_FOUND(HttpStatus.NOT_FOUND, "L001", "존재하지 않는 영상입니다."),
    COURSE_NOT_PUBLISHED(HttpStatus.FORBIDDEN, "L002", "공개되지 않은 강의입니다."),
    ENROLLMENT_REQUIRED(HttpStatus.FORBIDDEN, "L003", "수강권 또는 구독권이 필요합니다."),
    VIDEO_COMPLETION_CONDITION_NOT_MET(HttpStatus.CONFLICT, "L004", "영상 시청 비율이 완료 기준을 충족하지 않습니다."),
    VIDEO_PLAYBACK_URL_NOT_FOUND(HttpStatus.NOT_FOUND, "L005", "영상 재생 URL을 찾을 수 없습니다."),

    // 학습 통계 예외
    DAILY_STATS_MEMBER_ID_REQUIRED(HttpStatus.BAD_REQUEST, "DS001", "회원 ID는 필수입니다."),
    DAILY_STATS_DATE_REQUIRED(HttpStatus.BAD_REQUEST, "DS002", "조회 날짜는 필수입니다."),
    DAILY_STATS_DATE_FORMAT_INVALID(HttpStatus.BAD_REQUEST, "DS003", "조회 날짜는 yyyy-MM-dd 형식이어야 합니다."),
    DAILY_STATS_INVALID(HttpStatus.BAD_REQUEST, "DS004", "일별 학습 통계 정보가 올바르지 않습니다."),

    //게시글 예외
    SUBJECT_REQUIRED(HttpStatus.BAD_REQUEST, "P001", "과목을 선택하세요"),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "P003", "존재하지 않는 게시글입니다."),
    FILE_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "P002", "파일은 최대 2개까지 첨부 가능합니다."),
    POST_NOT_AUTHORIZED(HttpStatus.FORBIDDEN, "P004", "본인의 게시글만 수정/삭제할 수 있습니다."),
    POST_ACCEPTED_CANNOT_DELETE(HttpStatus.FORBIDDEN, "P005", "채택 완료된 게시글은 삭제할 수 없습니다."),
    POST_ACCEPTED_CANNOT_MODIFY(HttpStatus.FORBIDDEN, "P006", "채택 완료된 게시글은 수정할 수 없습니다."),

    // 이미지 파일 업로드 예외
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "F001", "jpg, jpeg, png 형식의 파일만 업로드 가능합니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "F002", "파일 크기는 5MB 이하여야 합니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "F003", "파일 업로드에 실패했습니다."),
    PROFILE_IMAGE_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "F004", "프로필 이미지는 1개만 업로드 가능합니다."),
    VIDEO_FILE_SIZE_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, "F005", "영상 파일 크기는 1GB 이하여야 합니다."),
    VIDEO_UPLOAD_NOT_FOUND(HttpStatus.BAD_REQUEST, "F006", "업로드된 영상을 찾을 수 없습니다. presign 발급 후 업로드를 먼저 완료해주세요."),

    //댓글 예외
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "C001", "존재하지 않는 댓글입니다."),
    REPLY_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "C002", "대댓글에는 답글을 달 수 없습니다."),
    ACCEPT_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "C003", "자유게시판에서는 채택할 수 없습니다."),
    ACCEPT_NOT_AUTHORIZED(HttpStatus.FORBIDDEN, "C004", "게시글 작성자만 채택할 수 있습니다."),
    ALREADY_ACCEPTED(HttpStatus.CONFLICT, "C005", "이미 채택된 댓글이 있습니다."),
    REPLY_CANNOT_BE_ACCEPTED(HttpStatus.BAD_REQUEST, "C006", "대댓글은 채택할 수 없습니다."),
    INVALID_PARENT_COMMENT(HttpStatus.BAD_REQUEST, "C009", "다른 게시글의 댓글에는 답글을 달 수 없습니다."),
    COMMENT_NOT_AUTHORIZED(HttpStatus.FORBIDDEN, "C007", "본인의 댓글만 수정/삭제할 수 있습니다."),
    COMMENT_ACCEPTED_CANNOT_MODIFY(HttpStatus.FORBIDDEN, "C008", "채택된 댓글은 수정할 수 없습니다."),
    COMMENT_ACCEPTED_CANNOT_DELETE(HttpStatus.FORBIDDEN, "C009", "채택된 댓글은 삭제할 수 없습니다."),

    //커뮤니티 공통 접근 예외
    COMMUNITY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "C010", "커뮤니티 이용 권한이 없습니다."),

    //공지 예외
    NOTICE_NOT_AUTHORIZED(HttpStatus.FORBIDDEN, "N001", "해당 강의의 담당 강사가 아닙니다."),
    COURSE_NOT_FOUND2(HttpStatus.NOT_FOUND, "CO001", "존재하지 않는 강의입니다."),
    NOTICE_ADMIN_ONLY(HttpStatus.FORBIDDEN, "N002", "관리자만 전체 공지사항을 작성할 수 있습니다."),
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "N003", "존재하지 않는 공지사항입니다."),
    COURSE_ID_REQUIRED(HttpStatus.BAD_REQUEST, "N004", "강의 공지 조회 시 강의 ID는 필수입니다."),

    /* =========================================================================
       7. 결제 관련 예외 (PAY000)
       ========================================================================= */
    DUPLICATE_PAYMENT_REQUEST(HttpStatus.CONFLICT, "P001", "이미 처리 중인 결제 요청입니다."),
    PG_TIMEOUT(HttpStatus.INTERNAL_SERVER_ERROR, "P002", "PG사 응답이 지연되어 결제에 실패했습니다."),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "P003", "결제 내역을 찾을 수 없습니다."),
    PAYMENT_NOT_REFUNDABLE(HttpStatus.CONFLICT, "P004", "결제완료 상태의 결제만 환불할 수 있습니다."),

    /* =========================================================================
       7-1. 주문 관련 예외 (ORDER000)
       ========================================================================= */
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORD001", "주문을 찾을 수 없습니다."),
    ORDER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "ORD002", "본인의 주문만 조회할 수 있습니다."),
    EMPTY_CHECKOUT(HttpStatus.BAD_REQUEST, "ORD003", "결제할 대상이 없습니다."),
    ORDER_ALREADY_PROCESSED(HttpStatus.CONFLICT, "ORD004", "이미 처리된 주문입니다."),
    ORDER_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "ORD005", "결제 금액이 주문 금액과 일치하지 않습니다."),
    ORDER_NOT_REFUNDABLE(HttpStatus.CONFLICT, "ORD006", "환불 가능한 상태의 주문이 아닙니다."),
    ORDER_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "ORD007", "주문 항목을 찾을 수 없습니다."),
    ORDER_ITEM_ALREADY_REFUNDED(HttpStatus.CONFLICT, "ORD008", "이미 환불된 주문 항목입니다."),
    ORDER_ENROLLMENT_REVOKE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ORD009", "수강 권한 박탈에 실패했습니다."),
    ORDER_INVALID_STATUS(HttpStatus.INTERNAL_SERVER_ERROR, "ORD010", "알 수 없는 주문 상태입니다."),
    // 순공 세션 관련 예외
    STUDY_TIMER_SESSION_ALREADY_RUNNING(HttpStatus.CONFLICT, "ST001", "이미 실행 중인 순공시간 세션이 있습니다."),
    STUDY_TIMER_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "ST002", "존재하지 않는 순공시간 세션입니다."),
    STUDY_TIMER_SESSION_NOT_RUNNING(HttpStatus.CONFLICT, "ST003", "실행 중인 순공시간 세션만 처리할 수 있습니다."),
    STUDY_TIMER_LOCK_TIMEOUT(HttpStatus.CONFLICT, "ST004", "순공시간 세션 처리 중입니다. 잠시 후 다시 시도해주세요."),
    STUDY_TIMER_MEMBER_ID_REQUIRED(HttpStatus.BAD_REQUEST, "ST005", "회원 ID는 필수입니다."),
    STUDY_TIMER_STARTED_AT_REQUIRED(HttpStatus.BAD_REQUEST, "ST006", "세션 시작 시각은 필수입니다."),
    STUDY_TIMER_SESSION_INVALID(HttpStatus.BAD_REQUEST, "ST007", "순공시간 세션 정보가 올바르지 않습니다."),
    STUDY_TIMER_HEARTBEAT_AT_REQUIRED(HttpStatus.BAD_REQUEST, "ST008", "하트비트 시각은 필수입니다."),
    STUDY_TIMER_HEARTBEAT_AT_IN_FUTURE(HttpStatus.BAD_REQUEST, "ST009", "하트비트 시각은 현재 시각 이후일 수 없습니다."),
    STUDY_TIMER_HEARTBEAT_AT_BEFORE_STARTED_AT(HttpStatus.BAD_REQUEST, "ST010", "하트비트 시각은 세션 시작 시각 이후여야 합니다."),
    STUDY_TIMER_ENDED_AT_REQUIRED(HttpStatus.BAD_REQUEST, "ST011", "세션 종료 시각은 필수입니다."),
    STUDY_TIMER_ENDED_AT_IN_FUTURE(HttpStatus.BAD_REQUEST, "ST012", "세션 종료 시각은 현재 시각 이후일 수 없습니다."),
    STUDY_TIMER_ENDED_AT_BEFORE_STARTED_AT(HttpStatus.BAD_REQUEST, "ST013", "세션 종료 시각은 세션 시작 시각 이후여야 합니다."),
    STUDY_TIMER_DAILY_STAT_INVALID(HttpStatus.BAD_REQUEST, "ST014", "일별 순공시간 통계 정보가 올바르지 않습니다."),
    STUDY_TIMER_DAILY_START_DATE_REQUIRED(HttpStatus.BAD_REQUEST, "ST015", "시작 날짜는 필수입니다."),
    STUDY_TIMER_DAILY_END_DATE_REQUIRED(HttpStatus.BAD_REQUEST, "ST016", "종료 날짜는 필수입니다."),
    STUDY_TIMER_DAILY_DATE_RANGE_INVALID(HttpStatus.BAD_REQUEST, "ST017", "시작 날짜는 종료 날짜 이후일 수 없습니다."),
    STUDY_TIMER_DAILY_DATE_RANGE_TOO_LONG(HttpStatus.BAD_REQUEST, "ST018", "일별 학습 시간은 최대 1년치까지만 조회할 수 있습니다."),
    STUDY_TIMER_DAILY_START_DATE_FORMAT_INVALID(HttpStatus.BAD_REQUEST, "ST019", "시작 날짜는 yyyy-MM-dd 형식이어야 합니다."),
    STUDY_TIMER_DAILY_END_DATE_FORMAT_INVALID(HttpStatus.BAD_REQUEST, "ST020", "종료 날짜는 yyyy-MM-dd 형식이어야 합니다."),
    STUDY_TIMER_PAUSED_AT_REQUIRED(HttpStatus.BAD_REQUEST, "ST021", "세션 일시정지 시각은 필수입니다."),
    STUDY_TIMER_PAUSED_AT_IN_FUTURE(HttpStatus.BAD_REQUEST, "ST022", "세션 일시정지 시각은 현재 시각 이후일 수 없습니다."),
    STUDY_TIMER_PAUSED_AT_BEFORE_STARTED_AT(HttpStatus.BAD_REQUEST, "ST023", "세션 일시정지 시각은 세션 시작 시각 이후여야 합니다."),
    STUDY_TIMER_SESSION_NOT_PAUSED(HttpStatus.CONFLICT, "ST024", "일시정지된 순공시간 세션만 재개할 수 있습니다."),
    STUDY_TIMER_RESUMED_AT_REQUIRED(HttpStatus.BAD_REQUEST, "ST025", "세션 재개 시각은 필수입니다."),
    STUDY_TIMER_RESUMED_AT_IN_FUTURE(HttpStatus.BAD_REQUEST, "ST026", "세션 재개 시각은 현재 시각 이후일 수 없습니다."),
    STUDY_TIMER_RESUMED_AT_BEFORE_PAUSED_AT(HttpStatus.BAD_REQUEST, "ST027", "세션 재개 시각은 세션 일시정지 시각 이후여야 합니다."),
    // 신고 예외
    REPORT_ALREADY_EXISTS(HttpStatus.CONFLICT, "RP001", "이미 신고한 대상입니다."),
    REPORT_TARGET_NOT_FOUND(HttpStatus.NOT_FOUND, "RP002", "존재하지 않거나 이미 삭제된 대상입니다."),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "RP003", "신고를 찾을 수 없습니다."),
    REPORT_ALREADY_PROCESSED(HttpStatus.CONFLICT, "RP004", "이미 처리된 신고입니다."),
    REPORT_TARGET_ALREADY_DELETED(HttpStatus.CONFLICT, "RP005", "이미 삭제된 신고 대상 콘텐츠입니다."),

    //알림 예외
    INVALID_NOTIFICATION(HttpStatus.BAD_REQUEST, "NT001", "알림 생성에 필요한 값이 누락되었습니다."),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NT002", "존재하지 않는 알림입니다."),
    INVALID_NOTICE_TYPE(HttpStatus.BAD_REQUEST, "N005", "유효하지 않은 공지 타입입니다."),

    /* =========================================================================
       장바구니 관련 예외 (CART000)
       ========================================================================= */
    CART_ITEM_ALREADY_EXISTS(HttpStatus.CONFLICT, "CART001", "이미 장바구니에 담긴 강의입니다."),
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "CART002", "장바구니에 해당 강의가 없습니다."),

    /* =========================================================================
       찜하기 관련 예외 (WISH000)
       ========================================================================= */
    WISHLIST_ITEM_ALREADY_EXISTS(HttpStatus.CONFLICT, "WISH001", "이미 찜한 강의입니다."),
    WISHLIST_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "WISH002", "찜 목록에 해당 강의가 없습니다."),

    /* =========================================================================
       구독(연간 패스) 관련 예외 (SUB000)
       ========================================================================= */
    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "SUB001", "구독 정보를 찾을 수 없습니다."),
    SUBSCRIPTION_ALREADY_ACTIVE(HttpStatus.CONFLICT, "SUB002", "이미 구독 중입니다."),

    /* =========================================================================
       스터디 모집 관련 예외 (SG000) — ST는 순공시간(study_timer)이 선점하여 SG 사용
       ========================================================================= */
    STUDY_MIN_COUNT_INVALID(HttpStatus.BAD_REQUEST, "SG001", "스터디 정원은 최소 2명 이상이어야 합니다.");


    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
