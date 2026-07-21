package com.wanted.backend.domain.study_timer.presentation.api;

import com.wanted.backend.domain.study_timer.application.command.EndStudyTimerSessionCommand;
import com.wanted.backend.domain.study_timer.application.command.PauseStudyTimerSessionCommand;
import com.wanted.backend.domain.study_timer.application.command.ResumeStudyTimerSessionCommand;
import com.wanted.backend.domain.study_timer.application.command.SaveStudyTimerHeartbeatCommand;
import com.wanted.backend.domain.study_timer.application.command.StartStudyTimerSessionCommand;
import com.wanted.backend.domain.study_timer.application.query.GetCurrentStudyTimerSessionQuery;
import com.wanted.backend.domain.study_timer.application.query.GetStudyTimerSessionsByDateQuery;
import com.wanted.backend.domain.study_timer.application.usecase.EndStudyTimerSessionUseCase;
import com.wanted.backend.domain.study_timer.application.usecase.GetCurrentStudyTimerSessionUseCase;
import com.wanted.backend.domain.study_timer.application.usecase.GetStudyTimerSessionsByDateUseCase;
import com.wanted.backend.domain.study_timer.application.usecase.PauseStudyTimerSessionUseCase;
import com.wanted.backend.domain.study_timer.application.usecase.ResumeStudyTimerSessionUseCase;
import com.wanted.backend.domain.study_timer.application.usecase.SaveStudyTimerHeartbeatUseCase;
import com.wanted.backend.domain.study_timer.application.usecase.StartStudyTimerSessionUseCase;
import com.wanted.backend.domain.study_timer.presentation.api.request.EndStudyTimerSessionRequest;
import com.wanted.backend.domain.study_timer.presentation.api.request.PauseStudyTimerSessionRequest;
import com.wanted.backend.domain.study_timer.presentation.api.request.ResumeStudyTimerSessionRequest;
import com.wanted.backend.domain.study_timer.presentation.api.request.SaveStudyTimerHeartbeatRequest;
import com.wanted.backend.domain.study_timer.presentation.api.request.StartStudyTimerSessionRequest;
import com.wanted.backend.domain.study_timer.presentation.api.response.CurrentStudyTimerSessionResponse;
import com.wanted.backend.domain.study_timer.presentation.api.response.DailyStudyTimerSessionResponse;
import com.wanted.backend.domain.study_timer.presentation.api.response.StudyTimerHeartbeatResponse;
import com.wanted.backend.domain.study_timer.presentation.api.response.StudyTimerSessionEndResponse;
import com.wanted.backend.domain.study_timer.presentation.api.response.StudyTimerSessionPauseResponse;
import com.wanted.backend.domain.study_timer.presentation.api.response.StudyTimerSessionResumeResponse;
import com.wanted.backend.domain.study_timer.presentation.api.response.StudyTimerSessionStartResponse;
import com.wanted.backend.global.common.ApiResponse;
import com.wanted.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/study-timers/sessions")
@Tag(name = "Study Timer", description = "순공시간 세션 API")
public class StudyTimerSessionController {

    private final StartStudyTimerSessionUseCase startStudyTimerSessionUseCase;
    private final SaveStudyTimerHeartbeatUseCase saveStudyTimerHeartbeatUseCase;
    private final PauseStudyTimerSessionUseCase pauseStudyTimerSessionUseCase;
    private final ResumeStudyTimerSessionUseCase resumeStudyTimerSessionUseCase;
    private final EndStudyTimerSessionUseCase endStudyTimerSessionUseCase;
    private final GetCurrentStudyTimerSessionUseCase getCurrentStudyTimerSessionUseCase;
    private final GetStudyTimerSessionsByDateUseCase getStudyTimerSessionsByDateUseCase;
    // '오늘' 기본값을 서버 JVM 타임존이 아닌 팀 표준 Clock(Asia/Seoul)으로 계산한다.
    private final Clock clock;

    @GetMapping
    @Operation(
            summary = "특정 날짜의 순공시간 세션 목록 조회",
            description = "타임테이블에 실제 공부 시간대를 막대로 표시하기 위한, 해당 날짜의 종료된(ENDED) 세션 목록입니다. "
                    + "date 미지정 시 오늘(KST). 진행 중 세션은 /sessions/current, 중단 세션은 제외됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "세션 목록 조회 성공 (없으면 빈 배열)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<ApiResponse<List<DailyStudyTimerSessionResponse>>> sessionsByDate(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "조회 날짜(ISO). 미지정 시 오늘(KST)", example = "2026-07-21")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate target = date != null ? date : LocalDate.now(clock);
        List<DailyStudyTimerSessionResponse> body =
                getStudyTimerSessionsByDateUseCase.handle(
                                new GetStudyTimerSessionsByDateQuery(userDetails.getMemberId(), target))
                        .stream()
                        .map(DailyStudyTimerSessionResponse::from)
                        .toList();

        return ApiResponse.success("순공시간 세션 목록을 조회했습니다.", body);
    }

    @GetMapping("/current")
    @Operation(
            summary = "실행 중 순공시간 세션 조회",
            description = "현재 로그인 사용자의 실행 중인 순공시간 세션을 조회합니다. 실행 중인 세션이 없으면 data는 null입니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "실행 중 세션 조회 성공 (세션 없으면 data: null)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<ApiResponse<CurrentStudyTimerSessionResponse>> current(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        GetCurrentStudyTimerSessionUseCase.CurrentStudyTimerSessionView result =
                getCurrentStudyTimerSessionUseCase.handle(new GetCurrentStudyTimerSessionQuery(
                        userDetails.getMemberId()
                ));

        return ApiResponse.success(
                "실행 중인 순공시간 세션을 조회했습니다.",
                CurrentStudyTimerSessionResponse.from(result)
        );
    }

    @PostMapping
    @Operation(
            summary = "순공시간 세션 시작",
            description = "순공시간 측정을 시작하고 실행 중인 세션 정보를 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "세션 시작 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 시작 시각"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 실행 중인 세션이 존재함")
    })
    public ResponseEntity<ApiResponse<StudyTimerSessionStartResponse>> start(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody StartStudyTimerSessionRequest request
    ) {
        StartStudyTimerSessionUseCase.StudyTimerSessionStartView result =
                startStudyTimerSessionUseCase.handle(new StartStudyTimerSessionCommand(
                        userDetails.getMemberId(),
                        request.startedAt()
                ));

        return ApiResponse.success(
                "순공시간 측정을 시작했습니다.",
                StudyTimerSessionStartResponse.from(result)
        );
    }

    @PatchMapping("/{sessionId}/heartbeat")
    @Operation(
            summary = "순공시간 하트비트 저장",
            description = "실행 중인 순공시간 세션의 중간 경과 시간을 저장합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "하트비트 저장 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 하트비트 시각"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 세션이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "세션이 실행 중 상태가 아님")
    })
    public ResponseEntity<ApiResponse<StudyTimerHeartbeatResponse>> heartbeat(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "하트비트를 저장할 세션 ID", example = "55")
            @Positive
            @PathVariable Long sessionId,
            @Valid @RequestBody SaveStudyTimerHeartbeatRequest request
    ) {
        SaveStudyTimerHeartbeatUseCase.StudyTimerHeartbeatView result =
                saveStudyTimerHeartbeatUseCase.handle(new SaveStudyTimerHeartbeatCommand(
                        userDetails.getMemberId(),
                        sessionId,
                        request.heartbeatAt()
                ));

        return ApiResponse.success(
                "순공시간 하트비트를 저장했습니다.",
                StudyTimerHeartbeatResponse.from(result)
        );
    }

    @PatchMapping("/{sessionId}/pause")
    @Operation(
            summary = "순공시간 세션 일시정지",
            description = "실행 중인 순공시간 세션을 일시정지하고 누적 순공시간을 저장합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "세션 일시정지 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 일시정지 시각"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 세션이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "세션이 실행 중 상태가 아님")
    })
    public ResponseEntity<ApiResponse<StudyTimerSessionPauseResponse>> pause(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "일시정지할 세션 ID", example = "55")
            @Positive
            @PathVariable Long sessionId,
            @Valid @RequestBody PauseStudyTimerSessionRequest request
    ) {
        PauseStudyTimerSessionUseCase.StudyTimerSessionPauseView result =
                pauseStudyTimerSessionUseCase.handle(new PauseStudyTimerSessionCommand(
                        userDetails.getMemberId(),
                        sessionId,
                        request.pausedAt()
                ));

        return ApiResponse.success(
                "순공시간 측정을 일시정지했습니다.",
                StudyTimerSessionPauseResponse.from(result)
        );
    }

    @PatchMapping("/{sessionId}/resume")
    @Operation(
            summary = "순공시간 세션 재개",
            description = "일시정지된 순공시간 세션을 다시 실행 중 상태로 전환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "세션 재개 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 재개 시각"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 세션이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "세션이 일시정지 상태가 아님")
    })
    public ResponseEntity<ApiResponse<StudyTimerSessionResumeResponse>> resume(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "재개할 세션 ID", example = "55")
            @Positive
            @PathVariable Long sessionId,
            @Valid @RequestBody ResumeStudyTimerSessionRequest request
    ) {
        ResumeStudyTimerSessionUseCase.StudyTimerSessionResumeView result =
                resumeStudyTimerSessionUseCase.handle(new ResumeStudyTimerSessionCommand(
                        userDetails.getMemberId(),
                        sessionId,
                        request.resumedAt()
                ));

        return ApiResponse.success(
                "순공시간 측정을 재개했습니다.",
                StudyTimerSessionResumeResponse.from(result)
        );
    }

    @PatchMapping("/{sessionId}/end")
    @Operation(
            summary = "순공시간 세션 종료",
            description = "실행 중인 순공시간 세션을 종료하고 최종 누적 순공시간을 저장합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "세션 종료 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 종료 시각"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 세션이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 종료된 세션")
    })
    public ResponseEntity<ApiResponse<StudyTimerSessionEndResponse>> end(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "종료할 세션 ID", example = "55")
            @Positive
            @PathVariable Long sessionId,
            @Valid @RequestBody EndStudyTimerSessionRequest request
    ) {
        EndStudyTimerSessionUseCase.StudyTimerSessionEndView result =
                endStudyTimerSessionUseCase.handle(new EndStudyTimerSessionCommand(
                        userDetails.getMemberId(),
                        sessionId,
                        request.endedAt()
                ));

        return ApiResponse.success(
                "순공시간 측정을 종료했습니다.",
                StudyTimerSessionEndResponse.from(result)
        );
    }
}
