package com.wanted.backend.global.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("지원하지 않는 HTTP 메서드 요청은 500이 아니라 405(C012)로 응답한다")
    void methodNotSupportedReturns405NotServerError() {
        // 실측(traceId d7f6f93d): POST 전용 /api/similar-quizzes에 GET이 들어와
        // catch-all(Exception)로 떨어져 C002 500으로 위장 응답되던 회귀를 방지한다.
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/similar-quizzes");
        HttpRequestMethodNotSupportedException exception =
                new HttpRequestMethodNotSupportedException("GET");

        ResponseEntity<ErrorResponse> response = handler.handleMethodNotSupported(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo(ErrorCode.METHOD_NOT_ALLOWED.getCode());
        assertThat(response.getBody().getPath()).isEqualTo("/api/similar-quizzes");
    }
}
