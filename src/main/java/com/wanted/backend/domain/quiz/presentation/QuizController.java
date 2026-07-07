package com.wanted.backend.domain.quiz.presentation;

import com.wanted.backend.domain.quiz.application.command.CreateQuizCommand;
import com.wanted.backend.domain.quiz.application.command.DeleteQuizCommand;
import com.wanted.backend.domain.quiz.application.command.QuizQuestionCommand;
import com.wanted.backend.domain.quiz.application.command.UpdateQuizCommand;
import com.wanted.backend.domain.quiz.application.result.InstructorQuizDetail;
import com.wanted.backend.domain.quiz.application.result.InstructorQuizSummary;
import com.wanted.backend.domain.quiz.application.usecase.QuizCommandUseCase;
import com.wanted.backend.domain.quiz.application.usecase.QuizQueryUseCase;
import com.wanted.backend.global.common.ApiResponse;
import com.wanted.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Quiz", description = "학생/강사 퀴즈 API")
public class QuizController {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final QuizCommandUseCase quizCommandUseCase;
    private final QuizQueryUseCase quizQueryUseCase;

    @GetMapping("/members/me/quizzes")
    @Operation(summary = "내 퀴즈 목록 조회", description = "현재 로그인한 회원의 퀴즈 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<MyQuizListResponse>> getMyQuizzes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        List<MyQuizListResponse.MyQuizItem> quizzes = myQuizItems();

        MyQuizListResponse response = new MyQuizListResponse(
                paginate(quizzes, normalizedPage, normalizedSize),
                quizzes.size(),
                normalizedPage,
                normalizedSize
        );

        return ApiResponse.success("내 퀴즈 목록을 조회했습니다.", response);
    }

    @GetMapping("/quizzes/{quizId}")
    @Operation(summary = "학생 퀴즈 상세 조회", description = "학생이 풀이할 퀴즈 상세 정보와 문항 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<StudentQuizDetailResponse>> getStudentQuizDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long quizId
    ) {
        List<StudentQuizDetailResponse.Question> questions = studentQuizQuestions();

        StudentQuizDetailResponse response = new StudentQuizDetailResponse(
                quizId,
                "React 기초 개념 퀴즈",
                "React 완벽 가이드",
                "섹션 1: React 기초",
                questions.size(),
                3,
                false,
                questions
        );

        return ApiResponse.success("퀴즈 상세 정보를 조회했습니다.", response);
    }

    @PostMapping("/quizzes/{quizId}/submissions")
    @Operation(summary = "퀴즈 답안 제줄", description = "학생의 퀴즈 답안을 제출하고 재점 결과를 반환합니다.")
    public ResponseEntity<ApiResponse<QuizSubmissionResponse>> submitQuiz(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long quizId,
            @RequestBody(required = false) QuizSubmissionRequest request
    ) {
        QuizSubmissionResponse response = new QuizSubmissionResponse(
                55L,
                quizId,
                75,
                8,
                6,
                2,
                OffsetDateTime.parse("2026-05-10T15:30:00+09:00")
        );

        return ApiResponse.success("퀴즈가 제출되었습니다.", response);
    }

    @GetMapping("/quizzes/{quizId}/reports/me")
    @Operation(summary = "내 퀴즈 리포트 조회", description = "현재 로그인한 회원의 퀴즈 제출 결과와 해설 리포트를 조회합니다.")
    public ResponseEntity<ApiResponse<QuizReportResponse>> getMyQuizReport(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long quizId
    ) {
        List<QuizReportResponse.QuestionResult> questions = quizReportQuestions();

        QuizReportResponse response = new QuizReportResponse(
                quizId,
                "React 기초 개념 퀴즈",
                "React 완벽 가이드",
                "섹션 1: React 기초",
                75,
                questions.size(),
                6,
                2,
                OffsetDateTime.parse("2026-05-10T15:30:00+09:00"),
                questions
        );

        return ApiResponse.success("오답노트 및 리포트를 조회했습니다.", response);
    }

    @GetMapping("/instructor/quizzes")
    @Operation(summary = "강사 퀴즈 목록 조회", description = "강사가 관리하는 퀴즈 목록을 강의/섹션 기준으로 조회합니다")
    public ResponseEntity<ApiResponse<InstructorQuizListResponse>> getInstructorQuizzes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long sectionId
    ) {
        List<InstructorQuizSummary> summaries = quizQueryUseCase
                .getInstructorQuizzes(userDetails.getMemberId(), courseId, sectionId);

        InstructorQuizListResponse response = new InstructorQuizListResponse(
                courseId,
                sectionId,
                summaries.stream()
                        .map(summary -> new InstructorQuizListResponse.InstructorQuizItem(
                                summary.quizId(),
                                summary.quizTitle(),
                                summary.courseTitle(),
                                summary.sectionTitle(),
                                summary.questionCount(),
                                summary.createdAt().atZone(ZoneId.systemDefault()).toOffsetDateTime()))
                        .toList()
        );

        return ApiResponse.success("강사 퀴즈 목록을 조회했습니다.", response);
    }

    @GetMapping("/instructor/quizzes/{quizId}")
    @Operation(summary = "강사 퀴즈 상세 조회", description = "강사가 관리하는 퀴즈 상세 정보와 정답 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<InstructorQuizDetailResponse>> getInstructorQuizDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long quizId
    ) {
        InstructorQuizDetail detail = quizQueryUseCase
                .getInstructorQuizDetail(userDetails.getMemberId(), quizId);

        InstructorQuizDetailResponse response = new InstructorQuizDetailResponse(
                detail.quizId(),
                detail.quizTitle(),
                detail.courseId(),
                detail.courseTitle(),
                detail.sectionId(),
                detail.sectionTitle(),
                detail.questionCount(),
                detail.createdAt().atZone(ZoneId.systemDefault()).toOffsetDateTime(),
                detail.questions().stream()
                        .map(question -> new InstructorQuizDetailResponse.Question(
                                question.questionId(),
                                question.questionNumber(),
                                question.questionText(),
                                question.correctOptionId(),
                                question.explanation(),
                                question.options().stream()
                                        .map(option -> new InstructorQuizDetailResponse.Option(
                                                option.optionId(),
                                                option.optionNumber(),
                                                option.optionText(),
                                                option.correct()))
                                        .toList()))
                        .toList()
        );

        return ApiResponse.success("강사 퀴즈 상세 정보를 조회했습니다.", response);
    }

    @PostMapping("/instructor/quizzes")
    @Operation(summary = "강사 퀴즈 등록", description = "강사가 강의/섹션에 연결된 퀴즈를 등록합니다.")
    public ResponseEntity<ApiResponse<InstructorQuizMutationResponse>> createInstructorQuiz(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody InstructorQuizRequest request
    ) {
        CreateQuizCommand command = new CreateQuizCommand(
                userDetails.getMemberId(),
                request.courseId(),
                request.sectionId(),
                request.quizTitle(),
                toQuestionCommands(request.questions())
        );

        Long quizId = quizCommandUseCase.create(command);

        InstructorQuizMutationResponse response = new InstructorQuizMutationResponse(
                quizId,
                request.quizTitle(),
                request.questions().size(),
                OffsetDateTime.now()
        );

        return ApiResponse.created("퀴즈가 등록되었습니다.", response);
    }

    @PutMapping("/instructor/quizzes/{quizId}")
    @Operation(summary = "강사 퀴즈 수정", description = "강사가 등록한 퀴즈의 제목과 문항 정보를 수정합니다")
    public ResponseEntity<ApiResponse<InstructorQuizMutationResponse>> updateInstructorQuiz(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long quizId,
            @Valid @RequestBody InstructorQuizRequest request
    ) {
        UpdateQuizCommand command = new UpdateQuizCommand(
                quizId,
                userDetails.getMemberId(),
                request.courseId(),
                request.sectionId(),
                request.quizTitle(),
                toQuestionCommands(request.questions())
        );

        quizCommandUseCase.update(command);

        InstructorQuizMutationResponse response = new InstructorQuizMutationResponse(
                quizId,
                request.quizTitle(),
                request.questions().size(),
                OffsetDateTime.now()
        );

        return ApiResponse.success("퀴즈가 수정되었습니다.", response);
    }

    @DeleteMapping("/instructor/quizzes/{quizId}")
    @Operation(summary = "강사 퀴즈 삭제", description = "강사가 등록한 퀴즈를 삭제합니다.")
    public ResponseEntity<ApiResponse<InstructorQuizDeleteResponse>> deleteInstructorQuiz(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long quizId
    ) {
        quizCommandUseCase.delete(new DeleteQuizCommand(quizId, userDetails.getMemberId()));

        InstructorQuizDeleteResponse response = new InstructorQuizDeleteResponse(
                quizId,
                "DELETED",
                OffsetDateTime.now()
        );

        return ApiResponse.success("퀴즈가 삭제되었습니다.", response);
    }

    @GetMapping("/instructors/me/quizzes/{quizId}/statistics")
    @Operation(summary = "강사 퀴즈 통계 조회", description = "강사가 관리하는 퀴즈의 응시 현황과 점수 분포를 조회합니다.")
    public ResponseEntity<ApiResponse<InstructorQuizStatisticsResponse>> getInstructorQuizStatistics(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long quizId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        InstructorQuizStatisticsResponse statistics = instructorQuizStatistics();
        InstructorQuizStatisticsResponse response = new InstructorQuizStatisticsResponse(
                statistics.courseTitle(),
                statistics.sectionTitle(),
                statistics.quizTitle(),
                statistics.summary(),
                statistics.scoreDistribution(),
                paginate(statistics.students(), normalizedPage, normalizedSize)
        );

        return ApiResponse.success("퀴즈 점수 현황을 조회했습니다.", response);
    }

    private List<QuizQuestionCommand> toQuestionCommands(List<InstructorQuizRequest.Question> questions) {
        return questions.stream()
                .map(q -> new QuizQuestionCommand(
                        q.questionText(),
                        q.explanation(),
                        q.correctOptionNumber(),
                        q.options().stream().map(InstructorQuizRequest.Option::optionText).toList()
                ))
                .toList();
    }

    private int normalizePage(Integer page) {
        if (page == null || page < DEFAULT_PAGE) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private <T> List<T> paginate(List<T> items, int page, int size) {
        long offset = (long) page * size;
        if (offset >= items.size()) {
            return List.of();
        }

        int fromIndex = (int) offset;
        int toIndex = Math.min(fromIndex + size, items.size());
        return items.subList(fromIndex, toIndex);
    }

    private List<MyQuizListResponse.MyQuizItem> myQuizItems() {
        return List.of(
                new MyQuizListResponse.MyQuizItem(90L, "React 기초 개념 퀴즈", 1L, "React 완벽 가이드", "섹션 1: React 기초", 8, true, 75, OffsetDateTime.parse("2026-05-10T15:30:00+09:00")),
                new MyQuizListResponse.MyQuizItem(91L, "Hooks 활용 퀴즈", 1L, "React 완벽 가이드", "섹션 2: Hooks", 6, false, null, null),
                new MyQuizListResponse.MyQuizItem(92L, "상태 관리 패턴 퀴즈", 1L, "React 완벽 가이드", "섹션 3: 상태 관리", 7, true, 92, OffsetDateTime.parse("2026-05-12T20:10:00+09:00")),
                new MyQuizListResponse.MyQuizItem(93L, "React Router 실전 퀴즈", 2L, "프론트엔드 라우팅 마스터", "섹션 2: 동적 라우팅", 5, false, null, null),
                new MyQuizListResponse.MyQuizItem(94L, "폼 검증과 에러 처리 퀴즈", 3L, "실전 프론트엔드 폼", "섹션 4: 유효성 검증", 9, true, 88, OffsetDateTime.parse("2026-05-18T21:45:00+09:00")),
                new MyQuizListResponse.MyQuizItem(95L, "Spring Boot REST API 퀴즈", 4L, "Spring Boot 핵심 가이드", "섹션 1: REST API 설계", 10, true, 70, OffsetDateTime.parse("2026-05-20T19:20:00+09:00")),
                new MyQuizListResponse.MyQuizItem(96L, "JPA 연관관계 퀴즈", 5L, "JPA 실전 입문", "섹션 3: 연관관계 매핑", 8, false, null, null),
                new MyQuizListResponse.MyQuizItem(97L, "SQL 기본기 퀴즈", 6L, "데이터베이스 첫걸음", "섹션 2: SELECT와 JOIN", 12, true, 95, OffsetDateTime.parse("2026-05-25T22:05:00+09:00")),
                new MyQuizListResponse.MyQuizItem(98L, "알고리즘 정렬 퀴즈", 7L, "코딩테스트 기본기", "섹션 1: 정렬", 6, true, 83, OffsetDateTime.parse("2026-05-28T18:30:00+09:00")),
                new MyQuizListResponse.MyQuizItem(99L, "Docker 배포 기초 퀴즈", 8L, "배포 자동화 입문", "섹션 1: 컨테이너 이해", 7, false, null, null),
                new MyQuizListResponse.MyQuizItem(100L, "Redis 캐싱 전략 퀴즈", 9L, "백엔드 성능 최적화", "섹션 2: 캐시 설계", 8, true, 78, OffsetDateTime.parse("2026-06-01T17:15:00+09:00")),
                new MyQuizListResponse.MyQuizItem(101L, "JWT 인증 흐름 퀴즈", 10L, "Spring Security 실전", "섹션 3: 토큰 인증", 9, false, null, null)
        );
    }

    private List<StudentQuizDetailResponse.Question> studentQuizQuestions() {
        return List.of(
                new StudentQuizDetailResponse.Question(1L, 1, "React의 가상 DOM이란 무엇인가요?", List.of(
                        new StudentQuizDetailResponse.Option(1L, 1, "실제 DOM의 복사본"),
                        new StudentQuizDetailResponse.Option(2L, 2, "메모리에 존재하는 DOM의 표현"),
                        new StudentQuizDetailResponse.Option(3L, 3, "HTML 파일"),
                        new StudentQuizDetailResponse.Option(4L, 4, "CSS 스타일시트")
                )),
                new StudentQuizDetailResponse.Question(2L, 2, "JSX는 무엇의 약자인가요?", List.of(
                        new StudentQuizDetailResponse.Option(5L, 1, "JavaScript XML"),
                        new StudentQuizDetailResponse.Option(6L, 2, "Java Syntax Extension"),
                        new StudentQuizDetailResponse.Option(7L, 3, "JSON XML"),
                        new StudentQuizDetailResponse.Option(8L, 4, "JavaScript Extension")
                )),
                new StudentQuizDetailResponse.Question(3L, 3, "useState 훅이 반환하는 값의 조합으로 올바른 것은 무엇인가요?", List.of(
                        new StudentQuizDetailResponse.Option(9L, 1, "현재 상태와 상태 변경 함수"),
                        new StudentQuizDetailResponse.Option(10L, 2, "렌더링 함수와 DOM 노드"),
                        new StudentQuizDetailResponse.Option(11L, 3, "이전 props와 현재 props"),
                        new StudentQuizDetailResponse.Option(12L, 4, "컴포넌트 이름과 이벤트 객체")
                )),
                new StudentQuizDetailResponse.Question(4L, 4, "useEffect의 의존성 배열을 빈 배열로 전달하면 언제 실행되나요?", List.of(
                        new StudentQuizDetailResponse.Option(13L, 1, "컴포넌트가 처음 마운트된 뒤 한 번"),
                        new StudentQuizDetailResponse.Option(14L, 2, "상태가 바뀔 때마다 항상"),
                        new StudentQuizDetailResponse.Option(15L, 3, "이벤트가 발생할 때마다"),
                        new StudentQuizDetailResponse.Option(16L, 4, "브라우저가 종료될 때 한 번")
                )),
                new StudentQuizDetailResponse.Question(5L, 5, "리스트 렌더링에서 key prop을 사용하는 주된 이유는 무엇인가요?", List.of(
                        new StudentQuizDetailResponse.Option(17L, 1, "CSS 우선순위를 높이기 위해"),
                        new StudentQuizDetailResponse.Option(18L, 2, "각 항목의 변경을 안정적으로 식별하기 위해"),
                        new StudentQuizDetailResponse.Option(19L, 3, "API 호출을 자동으로 캐싱하기 위해"),
                        new StudentQuizDetailResponse.Option(20L, 4, "컴포넌트를 서버에서만 렌더링하기 위해")
                )),
                new StudentQuizDetailResponse.Question(6L, 6, "제어 컴포넌트의 설명으로 가장 알맞은 것은 무엇인가요?", List.of(
                        new StudentQuizDetailResponse.Option(21L, 1, "DOM이 입력값을 직접 관리하는 컴포넌트"),
                        new StudentQuizDetailResponse.Option(22L, 2, "React 상태가 입력값을 관리하는 컴포넌트"),
                        new StudentQuizDetailResponse.Option(23L, 3, "서버 상태만 관리하는 컴포넌트"),
                        new StudentQuizDetailResponse.Option(24L, 4, "렌더링을 하지 않는 컴포넌트")
                )),
                new StudentQuizDetailResponse.Question(7L, 7, "props의 일반적인 역할은 무엇인가요?", List.of(
                        new StudentQuizDetailResponse.Option(25L, 1, "부모 컴포넌트에서 자식 컴포넌트로 데이터를 전달한다"),
                        new StudentQuizDetailResponse.Option(26L, 2, "브라우저 쿠키를 암호화한다"),
                        new StudentQuizDetailResponse.Option(27L, 3, "데이터베이스 트랜잭션을 관리한다"),
                        new StudentQuizDetailResponse.Option(28L, 4, "번들 파일을 압축한다")
                )),
                new StudentQuizDetailResponse.Question(8L, 8, "Context API를 사용하는 대표적인 목적은 무엇인가요?", List.of(
                        new StudentQuizDetailResponse.Option(29L, 1, "이미지 파일을 최적화하기 위해"),
                        new StudentQuizDetailResponse.Option(30L, 2, "깊은 컴포넌트 트리에 공통 상태를 전달하기 위해"),
                        new StudentQuizDetailResponse.Option(31L, 3, "HTTP 요청을 자동 재시도하기 위해"),
                        new StudentQuizDetailResponse.Option(32L, 4, "CSS 파일을 JavaScript로 변환하기 위해")
                ))
        );
    }

    private List<QuizReportResponse.QuestionResult> quizReportQuestions() {
        return List.of(
                new QuizReportResponse.QuestionResult(1L, 1, "React의 가상 DOM이란 무엇인가요?", 2L, 2L, true, "가상 DOM은 메모리에 존재하는 UI 표현이며 실제 DOM 변경을 효율화합니다.", List.of(
                        new QuizReportResponse.Option(1L, 1, "실제 DOM의 복사본"),
                        new QuizReportResponse.Option(2L, 2, "메모리에 존재하는 DOM의 표현"),
                        new QuizReportResponse.Option(3L, 3, "HTML 파일"),
                        new QuizReportResponse.Option(4L, 4, "CSS 스타일시트")
                )),
                new QuizReportResponse.QuestionResult(2L, 2, "JSX는 무엇의 약자인가요?", 5L, 6L, false, "JSX는 JavaScript XML의 약자로 JavaScript 안에서 UI 구조를 표현합니다.", List.of(
                        new QuizReportResponse.Option(5L, 1, "JavaScript XML"),
                        new QuizReportResponse.Option(6L, 2, "Java Syntax Extension"),
                        new QuizReportResponse.Option(7L, 3, "JSON XML"),
                        new QuizReportResponse.Option(8L, 4, "JavaScript Extension")
                )),
                new QuizReportResponse.QuestionResult(3L, 3, "useState 훅이 반환하는 값의 조합으로 올바른 것은 무엇인가요?", 9L, 9L, true, "useState는 현재 상태값과 해당 상태를 갱신하는 함수를 배열로 반환합니다.", List.of(
                        new QuizReportResponse.Option(9L, 1, "현재 상태와 상태 변경 함수"),
                        new QuizReportResponse.Option(10L, 2, "렌더링 함수와 DOM 노드"),
                        new QuizReportResponse.Option(11L, 3, "이전 props와 현재 props"),
                        new QuizReportResponse.Option(12L, 4, "컴포넌트 이름과 이벤트 객체")
                )),
                new QuizReportResponse.QuestionResult(4L, 4, "useEffect의 의존성 배열을 빈 배열로 전달하면 언제 실행되나요?", 13L, 13L, true, "빈 의존성 배열은 최초 마운트 이후 한 번만 effect를 실행하게 합니다.", List.of(
                        new QuizReportResponse.Option(13L, 1, "컴포넌트가 처음 마운트된 뒤 한 번"),
                        new QuizReportResponse.Option(14L, 2, "상태가 바뀔 때마다 항상"),
                        new QuizReportResponse.Option(15L, 3, "이벤트가 발생할 때마다"),
                        new QuizReportResponse.Option(16L, 4, "브라우저가 종료될 때 한 번")
                )),
                new QuizReportResponse.QuestionResult(5L, 5, "리스트 렌더링에서 key prop을 사용하는 주된 이유는 무엇인가요?", 18L, 18L, true, "key는 React가 리스트 항목의 추가, 삭제, 이동을 안정적으로 식별하도록 돕습니다.", List.of(
                        new QuizReportResponse.Option(17L, 1, "CSS 우선순위를 높이기 위해"),
                        new QuizReportResponse.Option(18L, 2, "각 항목의 변경을 안정적으로 식별하기 위해"),
                        new QuizReportResponse.Option(19L, 3, "API 호출을 자동으로 캐싱하기 위해"),
                        new QuizReportResponse.Option(20L, 4, "컴포넌트를 서버에서만 렌더링하기 위해")
                )),
                new QuizReportResponse.QuestionResult(6L, 6, "제어 컴포넌트의 설명으로 가장 알맞은 것은 무엇인가요?", 22L, 21L, false, "제어 컴포넌트는 입력값을 React state로 관리하고 변경 이벤트로 state를 갱신합니다.", List.of(
                        new QuizReportResponse.Option(21L, 1, "DOM이 입력값을 직접 관리하는 컴포넌트"),
                        new QuizReportResponse.Option(22L, 2, "React 상태가 입력값을 관리하는 컴포넌트"),
                        new QuizReportResponse.Option(23L, 3, "서버 상태만 관리하는 컴포넌트"),
                        new QuizReportResponse.Option(24L, 4, "렌더링을 하지 않는 컴포넌트")
                )),
                new QuizReportResponse.QuestionResult(7L, 7, "props의 일반적인 역할은 무엇인가요?", 25L, 25L, true, "props는 부모 컴포넌트가 자식 컴포넌트에 값을 전달하는 기본 수단입니다.", List.of(
                        new QuizReportResponse.Option(25L, 1, "부모 컴포넌트에서 자식 컴포넌트로 데이터를 전달한다"),
                        new QuizReportResponse.Option(26L, 2, "브라우저 쿠키를 암호화한다"),
                        new QuizReportResponse.Option(27L, 3, "데이터베이스 트랜잭션을 관리한다"),
                        new QuizReportResponse.Option(28L, 4, "번들 파일을 압축한다")
                )),
                new QuizReportResponse.QuestionResult(8L, 8, "Context API를 사용하는 대표적인 목적은 무엇인가요?", 30L, 30L, true, "Context API는 props drilling을 줄이고 여러 컴포넌트에 공통 값을 전달할 때 사용합니다.", List.of(
                        new QuizReportResponse.Option(29L, 1, "이미지 파일을 최적화하기 위해"),
                        new QuizReportResponse.Option(30L, 2, "깊은 컴포넌트 트리에 공통 상태를 전달하기 위해"),
                        new QuizReportResponse.Option(31L, 3, "HTTP 요청을 자동 재시도하기 위해"),
                        new QuizReportResponse.Option(32L, 4, "CSS 파일을 JavaScript로 변환하기 위해")
                ))
        );
    }

    private InstructorQuizStatisticsResponse instructorQuizStatistics() {
        return new InstructorQuizStatisticsResponse(
                "React 완벽 가이드",
                "섹션 1: React 기초",
                "React 기초 개념 퀴즈",
                new InstructorQuizStatisticsResponse.Summary(18, 4, 76),
                List.of(
                        new InstructorQuizStatisticsResponse.ScoreDistribution("90~100", 5, 28),
                        new InstructorQuizStatisticsResponse.ScoreDistribution("70~89", 7, 39),
                        new InstructorQuizStatisticsResponse.ScoreDistribution("50~69", 4, 22),
                        new InstructorQuizStatisticsResponse.ScoreDistribution("0~49", 2, 11)
                ),
                List.of(
                        new InstructorQuizStatisticsResponse.StudentScore("@choiaa2026", "최*아", true, 100, "2026-05-10"),
                        new InstructorQuizStatisticsResponse.StudentScore("@kimminsu92", "김*수", true, 95, "2026-05-10"),
                        new InstructorQuizStatisticsResponse.StudentScore("@leesj01", "이*진", true, 92, "2026-05-11"),
                        new InstructorQuizStatisticsResponse.StudentScore("@parkjh7", "박*현", true, 90, "2026-05-11"),
                        new InstructorQuizStatisticsResponse.StudentScore("@janghw44", "장*원", true, 96, "2026-05-12"),
                        new InstructorQuizStatisticsResponse.StudentScore("@jungym5", "정*민", true, 88, "2026-05-12"),
                        new InstructorQuizStatisticsResponse.StudentScore("@ohseul9", "오*슬", true, 85, "2026-05-12"),
                        new InstructorQuizStatisticsResponse.StudentScore("@moonhk2", "문*경", true, 83, "2026-05-13"),
                        new InstructorQuizStatisticsResponse.StudentScore("@limdo11", "임*윤", true, 82, "2026-05-13"),
                        new InstructorQuizStatisticsResponse.StudentScore("@baekhj3", "백*준", true, 78, "2026-05-14"),
                        new InstructorQuizStatisticsResponse.StudentScore("@seoha20", "서*아", true, 75, "2026-05-14"),
                        new InstructorQuizStatisticsResponse.StudentScore("@kangms8", "강*서", true, 72, "2026-05-14"),
                        new InstructorQuizStatisticsResponse.StudentScore("@hanjy31", "한*윤", true, 68, "2026-05-15"),
                        new InstructorQuizStatisticsResponse.StudentScore("@yuji77", "유*호", true, 64, "2026-05-15"),
                        new InstructorQuizStatisticsResponse.StudentScore("@shinra4", "신*라", true, 60, "2026-05-16"),
                        new InstructorQuizStatisticsResponse.StudentScore("@kwonbi6", "권*빈", true, 55, "2026-05-16"),
                        new InstructorQuizStatisticsResponse.StudentScore("@namjw13", "남*우", true, 48, "2026-05-17"),
                        new InstructorQuizStatisticsResponse.StudentScore("@choimh21", "조*현", true, 40, "2026-05-17"),
                        new InstructorQuizStatisticsResponse.StudentScore("@hanyeong3", "한*영", false, null, null),
                        new InstructorQuizStatisticsResponse.StudentScore("@songar12", "송*린", false, null, null),
                        new InstructorQuizStatisticsResponse.StudentScore("@yoonseo2", "윤*서", false, null, null),
                        new InstructorQuizStatisticsResponse.StudentScore("@minjun0", "민*준", false, null, null)
                )
        );
    }

    public record MyQuizListResponse(
            List<MyQuizItem> quizzes,
            int totalCount,
            int page,
            int size
    ) {
        public record MyQuizItem(
                Long quizId,
                String quizTitle,
                Long courseId,
                String courseTitle,
                String sectionTitle,
                int questionCount,
                boolean submitted,
                Integer score,
                OffsetDateTime submittedAt
        ) {
        }
    }

    public record StudentQuizDetailResponse(
            Long quizId,
            String quizTitle,
            String courseTitle,
            String sectionTitle,
            int totalQuestionCount,
            int answeredCount,
            boolean submitted,
            List<Question> questions
    ) {
        public record Question(Long questionId, int questionNumber, String questionText, List<Option> options) {
        }

        public record Option(Long optionId, int optionNumber, String optionText) {
        }
    }

    public record QuizSubmissionRequest(List<Answer> answers) {
        public record Answer(Long questionId, Long selectedOptionId) {
        }
    }

    public record QuizSubmissionResponse(
            Long submissionId,
            Long quizId,
            int score,
            int totalQuestionCount,
            int correctCount,
            int incorrectCount,
            OffsetDateTime submittedAt
    ) {
    }

    public record QuizReportResponse(
            Long quizId,
            String quizTitle,
            String courseTitle,
            String sectionTitle,
            int score,
            int totalQuestionCount,
            int correctCount,
            int incorrectCount,
            OffsetDateTime submittedAt,
            List<QuestionResult> questions
    ) {
        public record QuestionResult(
                Long questionId,
                int questionNumber,
                String questionText,
                Long correctOptionId,
                Long selectedOptionId,
                boolean correct,
                String explanation,
                List<Option> options
        ) {
        }

        public record Option(Long optionId, int optionNumber, String optionText) {
        }
    }

    public record InstructorQuizListResponse(
            Long courseId,
            Long sectionId,
            List<InstructorQuizItem> quizzes
    ) {
        public record InstructorQuizItem(
                Long quizId,
                String quizTitle,
                String courseTitle,
                String sectionTitle,
                int questionCount,
                OffsetDateTime createdAt
        ) {
        }
    }

    public record InstructorQuizDetailResponse(
            Long quizId,
            String quizTitle,
            Long courseId,
            String courseTitle,
            Long sectionId,
            String sectionTitle,
            int questionCount,
            OffsetDateTime createdAt,
            List<Question> questions
    ) {
        public record Question(
                Long questionId,
                int questionNumber,
                String questionText,
                Long correctOptionId,
                String explanation,
                List<Option> options
        ) {
        }

        public record Option(Long optionId, int optionNumber, String optionText, boolean correct) {
        }
    }

    public record InstructorQuizRequest(
            @NotBlank(message = "퀴즈 제목은 필수입니다.") String quizTitle,
            @NotNull(message = "강의 ID는 필수입니다.") Long courseId,
            @NotNull(message = "섹션 ID는 필수입니다.") Long sectionId,
            @NotEmpty(message = "문항은 최소 1개 이상이어야 합니다.") @Valid List<Question> questions
    ) {
        public record Question(
                @NotBlank(message = "문제 내용은 필수입니다.") String questionText,
                String explanation,
                @Min(value = 1, message = "정답 보기 번호는 1~4 사이여야 합니다.")
                @Max(value = 4, message = "정답 보기 번호는 1~4 사이여야 합니다.")
                @Schema(description = "정답 보기 번호(1~4)", example = "2") int correctOptionNumber,
                @Size(min = 4, max = 4, message = "보기는 4개가 필요합니다.") @Valid List<Option> options
        ) {
        }

        public record Option(@NotBlank(message = "보기 내용은 필수입니다.") String optionText) {
        }
    }

    public record InstructorQuizMutationResponse(
            Long quizId,
            String quizTitle,
            int questionCount,
            OffsetDateTime updatedAt
    ) {
    }

    public record InstructorQuizDeleteResponse(
            Long quizId,
            String status,
            OffsetDateTime deletedAt
    ) {
    }

    public record InstructorQuizStatisticsResponse(
            String courseTitle,
            String sectionTitle,
            String quizTitle,
            Summary summary,
            List<ScoreDistribution> scoreDistribution,
            List<StudentScore> students
    ) {
        public record Summary(int submittedCount, int notSubmittedCount, int averageScore) {
        }

        public record ScoreDistribution(String range, int count, int percentage) {
        }

        public record StudentScore(
                String userId,
                String name,
                boolean submitted,
                Integer score,
                String submittedAt
        ) {
        }
    }
}
