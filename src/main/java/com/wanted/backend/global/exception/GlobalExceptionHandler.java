package com.wanted.backend.global.exception;

import com.wanted.backend.domain.cource.domain.model.InvalidCoursePriceException;
import io.sentry.Sentry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * [1차 방어선] @Valid 유효성 검사 실패 예외 처리
     * 클라이언트가 값을 잘못 보낸 경우 (WARN 레벨)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException e,
            HttpServletRequest request) {

        // 에러가 난 필드와 원인을 Map에 담습니다
        Map<String, Object> details = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error ->
                details.putIfAbsent(error.getField(), error.getDefaultMessage())
        );

        log.warn("[Validation Error] Path: {}, Message: 클라이언트 입력값 오류", request.getRequestURI());

        ErrorResponse response = ErrorResponse.create()
                .errorCode(ErrorCode.INVALID_INPUT_VALUE.getCode())
                .message(ErrorCode.INVALID_INPUT_VALUE.getMessage())
                .path(request.getRequestURI())
                .details(details);

        return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getStatus()).body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestParameterException(
            MissingServletRequestParameterException e,
            HttpServletRequest request) {

        Map<String, Object> details = Map.of(e.getParameterName(), "필수 요청 파라미터입니다.");

        log.warn("[Missing Request Parameter] Path: {}, Parameter: {}", request.getRequestURI(), e.getParameterName());

        ErrorResponse response = ErrorResponse.create()
                .errorCode(ErrorCode.INVALID_INPUT_VALUE.getCode())
                .message(ErrorCode.INVALID_INPUT_VALUE.getMessage())
                .path(request.getRequestURI())
                .details(details);

        return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getStatus()).body(response);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException e,
            HttpServletRequest request) {

        log.warn("[File Size Error] Path: {}, Message: {}", request.getRequestURI(), e.getMessage());

        ErrorResponse response = ErrorResponse.create()
                .errorCode(ErrorCode.VIDEO_FILE_SIZE_EXCEEDED.getCode())
                .message(ErrorCode.VIDEO_FILE_SIZE_EXCEEDED.getMessage())
                .path(request.getRequestURI());

        return ResponseEntity.status(ErrorCode.VIDEO_FILE_SIZE_EXCEEDED.getStatus()).body(response);
    }

    /**
     * [2차 방어선-A] Course 도메인 가격 검증 예외
     */
    @ExceptionHandler(InvalidCoursePriceException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCoursePriceException(
            InvalidCoursePriceException e,
            HttpServletRequest request) {

        log.warn("[Domain Validation Error] Path: {}, Message: {}", request.getRequestURI(), e.getMessage());

        ErrorResponse response = ErrorResponse.create()
                .errorCode(ErrorCode.INVALID_COURSE_PRICE.getCode())
                .message(ErrorCode.INVALID_COURSE_PRICE.getMessage())
                .path(request.getRequestURI());

        return ResponseEntity.status(ErrorCode.INVALID_COURSE_PRICE.getStatus()).body(response);
    }

    /**
     * [2차 방어선-B] 도메인 규칙 위반 예외 처리
     * 도메인 모델(Course 등)이 던지는 IllegalArgumentException (WARN 레벨)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException e,
            HttpServletRequest request) {

        String message = "요청 파라미터 형식이 올바르지 않습니다.";
        if (e.getRequiredType() == LocalDate.class) {
            message = "날짜는 yyyy-MM-dd 형식이어야 합니다.";
        }

        log.warn("[Type Mismatch Error] Path: {}, Message: {}", request.getRequestURI(), message);

        ErrorResponse response = ErrorResponse.create()
                .errorCode(ErrorCode.INVALID_INPUT_VALUE.getCode())
                .message(message)
                .path(request.getRequestURI());

        return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getStatus()).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException e,
            HttpServletRequest request) {

        log.warn("[Domain Validation Error] Path: {}, Message: {}", request.getRequestURI(), e.getMessage());

        ErrorResponse response = ErrorResponse.create()
                .errorCode(ErrorCode.INVALID_INPUT_VALUE.getCode())
                .message(e.getMessage())
                .path(request.getRequestURI());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 요청 본문 파싱 실패 (JSON 형식 오류, enum 역직렬화 실패 등)
     * 예: 잘못된 subject/level enum 값 → @JsonCreator가 IllegalArgumentException을 던지면
     *     Jackson이 HttpMessageNotReadableException으로 감싸서 올라온다. 500이 아닌 400으로 처리.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadableException(
            HttpMessageNotReadableException e,
            HttpServletRequest request) {

        Throwable root = e.getMostSpecificCause();
        String message = (root != null && root.getMessage() != null)
                ? root.getMessage()
                : "요청 본문 형식이 올바르지 않습니다.";

        log.warn("[Message Not Readable] Path: {}, Message: {}", request.getRequestURI(), message);

        ErrorResponse response = ErrorResponse.create()
                .errorCode(ErrorCode.INVALID_INPUT_VALUE.getCode())
                .message(message)
                .path(request.getRequestURI());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * DB 제약 조건 위반 (unique, not null, data too long 등) → 400
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException e,
            HttpServletRequest request) {

        log.warn("[DataIntegrity Error] Path: {}, ExceptionType: {}", request.getRequestURI(), e.getMostSpecificCause().getClass().getSimpleName());

        ErrorResponse response = ErrorResponse.create()
                .errorCode(ErrorCode.INVALID_INPUT_VALUE.getCode())
                .message("입력값이 DB 제약 조건을 위반했습니다.")
                .path(request.getRequestURI());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * [3차 방어선] 비즈니스 로직 예외 처리
     * throw new BusinessException(...) 으로 던진 예외 (WARN 레벨)
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException e,
            HttpServletRequest request) {

        ErrorCode errorCode = e.getErrorCode();

        log.warn("[Business Error] Code: {}, Message: {}", errorCode.getCode(), errorCode.getMessage());

        ErrorResponse response = ErrorResponse.create()
                .errorCode(errorCode.getCode())
                .message(errorCode.getMessage())
                .path(request.getRequestURI());

        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException e,
            HttpServletRequest request) {

        log.warn("[Access Denied] Path: {}, Message: {}", request.getRequestURI(), e.getMessage());

        ErrorResponse response = ErrorResponse.create()
                .errorCode(ErrorCode.FORBIDDEN.getCode())
                .message(ErrorCode.FORBIDDEN.getMessage())
                .path(request.getRequestURI());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * [최종 방어선] 우리가 예상하지 못한 모든 시스템 장애
     * 500 에러는 ERROR 레벨로 찍어 error.log 파일로 직행시킵니다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllException(
            Exception e,
            HttpServletRequest request) {

        log.error("[System Error] Path: {}, Message: {}", request.getRequestURI(), e.getMessage(), e);
        Sentry.captureException(e);

        ErrorResponse response = ErrorResponse.create()
                .errorCode(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                .message(ErrorCode.INTERNAL_SERVER_ERROR.getMessage())
                .path(request.getRequestURI());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
