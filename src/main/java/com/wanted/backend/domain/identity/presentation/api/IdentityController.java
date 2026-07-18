package com.wanted.backend.domain.identity.presentation.api;

import com.wanted.backend.domain.identity.application.command.SignupCommand;
import com.wanted.backend.domain.identity.application.usecase.AuthCommandUseCase;
import com.wanted.backend.domain.identity.application.usecase.PreSignupImageUploadUseCase;
import com.wanted.backend.domain.identity.application.usecase.SignupCommandUseCase;
import com.wanted.backend.domain.identity.domain.model.AuthToken;
import com.wanted.backend.domain.identity.presentation.api.request.LoginRequest;
import com.wanted.backend.domain.identity.presentation.api.request.LogoutRequest;
import com.wanted.backend.domain.identity.presentation.api.request.RefreshTokenRequest;
import com.wanted.backend.domain.identity.presentation.api.request.SignupRequest;
import com.wanted.backend.domain.identity.presentation.api.response.DuplicateCheckResponse;
import com.wanted.backend.domain.identity.presentation.api.response.EmptyResponse;
import com.wanted.backend.domain.identity.presentation.api.response.LoginResponse;
import com.wanted.backend.domain.identity.presentation.api.response.PreSignupProfileImageResponse;
import com.wanted.backend.domain.identity.presentation.api.response.RefreshTokenResponse;
import com.wanted.backend.domain.identity.presentation.api.response.SignupResponse;
import com.wanted.backend.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Identity", description = "인증 및 회원가입 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class IdentityController {

    private final AuthCommandUseCase authCommandUseCase;
    private final SignupCommandUseCase signupCommandUseCase;
    private final PreSignupImageUploadUseCase preSignupImageUploadUseCase;

    @Operation(
            summary = "로그인",
            description = "아이디와 비밀번호로 로그인하고 Access Token과 Refresh Token을 발급합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "아이디 또는 비밀번호 불일치"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "계정 잠금 또는 정지 상태")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthToken token = authCommandUseCase.login(request.username(), request.password());

        String roleForClient = token.role().startsWith("ROLE_")
                ? token.role().substring(5)
                : token.role();

        return ApiResponse.success(
                "로그인에 성공했습니다",
                new LoginResponse(
                        token.accessToken(),
                        token.refreshToken(),
                        token.memberId(),
                        roleForClient
                )
        );
    }

    @Operation(
            summary = "Access Token 재발급",
            description = "Refresh Token을 검증하고 새로운 Access Token을 발급합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Access Token 재발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 Refresh Token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthToken token = authCommandUseCase.refresh(request.refreshToken());

        return ApiResponse.success(
                "Access Token이 재발급되었습니다",
                new RefreshTokenResponse(token.accessToken())
        );
    }

    @Operation(
            summary = "아이디 중복 확인",
            description = "회원가입에 사용할 아이디가 이미 사용 중인지 확인합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "중복 확인 완료")
    })
    @GetMapping("/check-username")
    public ResponseEntity<ApiResponse<DuplicateCheckResponse>> checkUsername(
            @Parameter(description = "중복 확인할 아이디", example = "testuser")
            @RequestParam String username
    ) {
        boolean isDuplicated = signupCommandUseCase.isUsernameDuplicated(username);
        String message = isDuplicated ? "사용이 불가능한 아이디입니다" : "사용 가능한 아이디입니다";

        return ApiResponse.success(message, new DuplicateCheckResponse(isDuplicated));
    }

    @Operation(
            summary = "이메일 중복 확인",
            description = "회원가입에 사용할 이메일이 이미 사용 중인지 확인합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "중복 확인 완료")
    })
    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<DuplicateCheckResponse>> checkEmail(
            @Parameter(description = "중복 확인할 이메일", example = "user@example.com")
            @RequestParam String email
    ) {
        boolean isDuplicated = signupCommandUseCase.isEmailDuplicated(email);
        String message = isDuplicated ? "사용이 불가능한 이메일입니다" : "사용 가능한 이메일입니다";

        return ApiResponse.success(message, new DuplicateCheckResponse(isDuplicated));
    }

    @Operation(
            summary = "회원가입",
            description = "이메일 인증을 완료한 사용자 정보를 기반으로 회원가입을 처리합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 검증 실패 또는 이메일 인증 토큰 유효하지 않음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "아이디 또는 이메일 중복")
    })
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        Long memberId = signupCommandUseCase.signup(new SignupCommand(
                request.username(),
                request.email(),
                request.password(),
                request.name(),
                request.gender(),
                request.birthDate(),
                request.phoneNumber(),
                request.profileImageUrl(),
                request.emailVerificationToken(),
                request.optionalTermsAgreed()
        ));

        return ApiResponse.created(
                "회원가입이 완료되었습니다",
                new SignupResponse(memberId)
        );
    }

    @Operation(
            summary = "가입 전 프로필 이미지 업로드",
            description = "로그인 없이 프로필 이미지를 업로드하고, 가입 API의 profileImageUrl에 그대로 넣을 저장 key와 "
                    + "미리보기용 URL을 반환합니다. 프론트는 응답의 key(URL 아님)를 signup 요청의 profileImageUrl로 전달합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "업로드 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "파일이 없거나 형식·크기 오류")
    })
    @PostMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PreSignupProfileImageResponse>> uploadSignupProfileImage(
            @RequestPart("profileImage") MultipartFile profileImage
    ) {
        PreSignupImageUploadUseCase.UploadedImageView uploaded =
                preSignupImageUploadUseCase.upload(profileImage);

        return ApiResponse.success(
                "프로필 이미지가 업로드되었습니다",
                new PreSignupProfileImageResponse(uploaded.key(), uploaded.previewUrl())
        );
    }

    @Operation(
            summary = "로그아웃",
            description = "Refresh Token을 삭제하여 로그아웃 처리합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (Access Token 없거나 만료)")
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<EmptyResponse>> logout(@Valid @RequestBody LogoutRequest request) {
        authCommandUseCase.logout(request.refreshToken());

        return ApiResponse.success("로그아웃되었습니다", new EmptyResponse());
    }
}
