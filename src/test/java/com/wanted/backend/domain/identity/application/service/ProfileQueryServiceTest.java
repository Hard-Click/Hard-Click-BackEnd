package com.wanted.backend.domain.identity.application.service;

import com.wanted.backend.domain.identity.application.port.ProfileImageStoragePort;
import com.wanted.backend.domain.identity.application.usecase.ProfileQueryUseCase.MyProfileView;
import com.wanted.backend.domain.identity.domain.model.Member;
import com.wanted.backend.domain.identity.domain.model.MemberStatus;
import com.wanted.backend.domain.identity.domain.model.Role;
import com.wanted.backend.domain.identity.domain.repository.MemberRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProfileQueryServiceTest {

    private MemberRepository memberRepository;
    private ProfileImageStoragePort profileImageStoragePort;
    private ProfileQueryService service;

    @BeforeEach
    void setUp() {
        memberRepository = mock(MemberRepository.class);
        profileImageStoragePort = mock(ProfileImageStoragePort.class);
        // publicUrl은 입력 key를 그대로 반환하도록 스텁(URL 조립 로직은 어댑터 단위 테스트 영역)
        when(profileImageStoragePort.publicUrl(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        service = new ProfileQueryService(memberRepository, profileImageStoragePort);
    }

    @Test
    void 내_프로필을_반환한다() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member("/profile.png")));

        MyProfileView result = service.handle(1L);

        assertThat(result.memberId()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("테스트유저");
        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.profileImageUrl()).isEqualTo("/profile.png");
    }

    @Test
    void 프로필_이미지가_없으면_기본_이미지를_반환한다() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member(null)));

        MyProfileView result = service.handle(1L);

        assertThat(result.profileImageUrl()).isEqualTo("/images/default-profile.png");
    }

    @Test
    void 회원이_존재하지_않으면_예외가_발생한다() {
        when(memberRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handle(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    private Member member(String profileImageUrl) {
        LocalDateTime now = LocalDateTime.now();
        return Member.restore(
                1L,
                "testuser",
                "test@example.com",
                "password",
                "테스트유저",
                "MALE",
                LocalDate.of(2000, 1, 1),
                "010-1234-5678",
                profileImageUrl,
                Role.STUDENT,
                MemberStatus.ACTIVE,
                false,
                0,
                false,
                null,
                null,
                now,
                now,
                false
        );
    }
}
