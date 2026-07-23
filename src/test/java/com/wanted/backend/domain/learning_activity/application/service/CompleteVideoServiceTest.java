package com.wanted.backend.domain.learning_activity.application.service;

import com.wanted.backend.domain.learning_activity.application.command.MemberVideoCommand;
import com.wanted.backend.domain.learning_activity.application.outbox.VideoCompletionOutboxStore;
import com.wanted.backend.domain.learning_activity.application.policy.VideoCompletionPolicy;
import com.wanted.backend.domain.learning_activity.application.port.VideoCatalogPort;
import com.wanted.backend.domain.learning_activity.domain.event.VideoCompletedEvent;
import com.wanted.backend.domain.learning_activity.domain.model.VideoAccessInfo;
import com.wanted.backend.domain.learning_activity.domain.model.VideoProgress;
import com.wanted.backend.domain.learning_activity.domain.repository.VideoProgressRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompleteVideoServiceTest {

    private VideoCatalogPort videoCatalogPort;
    private VideoProgressRepository videoProgressRepository;
    private VideoAccessService videoAccessService;
    private LearningActivityMetricRecorder metricRecorder;
    private ApplicationEventPublisher eventPublisher;
    private VideoCompletionOutboxStore videoCompletionOutboxStore;
    private CompleteVideoService service;

    @BeforeEach
    void setUp() {
        videoCatalogPort = mock(VideoCatalogPort.class);
        videoProgressRepository = mock(VideoProgressRepository.class);
        videoAccessService = mock(VideoAccessService.class);
        metricRecorder = mock(LearningActivityMetricRecorder.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        videoCompletionOutboxStore = mock(VideoCompletionOutboxStore.class);
        PlayableVideoProgressReader playableVideoProgressReader =
                new PlayableVideoProgressReader(videoCatalogPort, videoProgressRepository, videoAccessService);
        service = new CompleteVideoService(
                playableVideoProgressReader,
                videoProgressRepository,
                new VideoCompletionPolicy(),
                metricRecorder,
                eventPublisher,
                videoCompletionOutboxStore
        );
    }

    @Test
    void 시청_시간이_충분하면_영상을_완료_처리한다() {
        VideoAccessInfo accessInfo = accessInfo();
        VideoProgress progress = new VideoProgress(100L, 1L, 20L, 10L, 42, 270, false, null);
        when(videoCatalogPort.findByVideoId(10L)).thenReturn(Optional.of(accessInfo));
        when(videoProgressRepository.findByMemberIdAndVideoId(1L, 10L)).thenReturn(Optional.of(progress));

        service.handle(new MemberVideoCommand(1L, 10L));

        ArgumentCaptor<VideoProgress> captor = ArgumentCaptor.forClass(VideoProgress.class);
        verify(videoAccessService).validatePlayable(1L, accessInfo);
        verify(videoProgressRepository).save(captor.capture());
        assertThat(captor.getValue().completed()).isTrue();
        assertThat(captor.getValue().completedAt()).isNotNull();
        assertThat(captor.getValue().watchTimeSec()).isEqualTo(270);
        verify(metricRecorder).recordResult(LearningActivityAction.COMPLETE_VIDEO, null);
    }

    @Test
    void 메트릭_기록이_실패해도_영상_완료_처리는_그대로_유지된다() {
        VideoAccessInfo accessInfo = accessInfo();
        VideoProgress progress = new VideoProgress(100L, 1L, 20L, 10L, 42, 270, false, null);
        when(videoCatalogPort.findByVideoId(10L)).thenReturn(Optional.of(accessInfo));
        when(videoProgressRepository.findByMemberIdAndVideoId(1L, 10L)).thenReturn(Optional.of(progress));
        doThrow(new RuntimeException("metric registry down"))
                .when(metricRecorder).recordResult(LearningActivityAction.COMPLETE_VIDEO, null);

        assertThatCode(() -> service.handle(new MemberVideoCommand(1L, 10L)))
                .doesNotThrowAnyException();

        verify(videoProgressRepository).save(any(VideoProgress.class));
    }

    @Test
    void 배속_재생으로_시청_시간이_부족해도_재생_위치가_충분하면_완료_처리한다() {
        VideoAccessInfo accessInfo = accessInfo();
        VideoProgress progress = new VideoProgress(100L, 1L, 20L, 10L, 270, 150, false, null);
        when(videoCatalogPort.findByVideoId(10L)).thenReturn(Optional.of(accessInfo));
        when(videoProgressRepository.findByMemberIdAndVideoId(1L, 10L)).thenReturn(Optional.of(progress));

        service.handle(new MemberVideoCommand(1L, 10L));

        ArgumentCaptor<VideoProgress> captor = ArgumentCaptor.forClass(VideoProgress.class);
        verify(videoProgressRepository).save(captor.capture());
        assertThat(captor.getValue().completed()).isTrue();
        assertThat(captor.getValue().completedAt()).isNotNull();
        assertThat(captor.getValue().lastPositionSec()).isEqualTo(270);
        assertThat(captor.getValue().watchTimeSec()).isEqualTo(150);
    }

    @Test
    void 시청_시간이_부족하면_예외가_발생한다() {
        VideoAccessInfo accessInfo = accessInfo();
        VideoProgress progress = new VideoProgress(100L, 1L, 20L, 10L, 42, 269, false, null);
        when(videoCatalogPort.findByVideoId(10L)).thenReturn(Optional.of(accessInfo));
        when(videoProgressRepository.findByMemberIdAndVideoId(1L, 10L)).thenReturn(Optional.of(progress));

        assertThatThrownBy(() -> service.handle(new MemberVideoCommand(1L, 10L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VIDEO_COMPLETION_CONDITION_NOT_MET);

        verify(metricRecorder).recordResult(LearningActivityAction.COMPLETE_VIDEO, "VIDEO_COMPLETION_CONDITION_NOT_MET");
    }

    @Test
    void 영상_접근_정보가_없으면_예외가_발생한다() {
        when(videoCatalogPort.findByVideoId(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handle(new MemberVideoCommand(1L, 10L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VIDEO_NOT_FOUND);

        verify(metricRecorder).recordResult(LearningActivityAction.COMPLETE_VIDEO, "VIDEO_NOT_FOUND");
    }

    @Test
    void 영상_완료_처리_성공_시_VideoCompletedEvent를_발행한다() {
        VideoAccessInfo accessInfo = accessInfo();
        VideoProgress progress = new VideoProgress(100L, 1L, 20L, 10L, 42, 270, false, null);
        when(videoCatalogPort.findByVideoId(10L)).thenReturn(Optional.of(accessInfo));
        when(videoProgressRepository.findByMemberIdAndVideoId(1L, 10L)).thenReturn(Optional.of(progress));

        service.handle(new MemberVideoCommand(1L, 10L));

        ArgumentCaptor<VideoCompletedEvent> captor = ArgumentCaptor.forClass(VideoCompletedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().memberId()).isEqualTo(1L);
        assertThat(captor.getValue().videoId()).isEqualTo(10L);
        assertThat(captor.getValue().courseId()).isEqualTo(20L);
        // durable 경로: 완료 트랜잭션 안에서 outbox에도 적재된다(랭킹·잔디는 relay가 전달)
        verify(videoCompletionOutboxStore).enqueue(eq(1L), eq(10L), eq(20L), any());
    }

    @Test
    void 이미_완료된_영상을_다시_완료하면_이벤트를_재발행하지_않고_재저장도_하지_않는다() {
        VideoAccessInfo accessInfo = accessInfo();
        // completed=true 이되 진행값(100/300)은 완료 조건(임계 270) 미충족 → 완료 정책이 다시 실행되면 예외로 실패한다.
        // 따라서 이 테스트가 통과하려면 isCompleted() 가드가 정책 검사보다 먼저 조기종료해야만 한다.
        VideoProgress completed = new VideoProgress(100L, 1L, 20L, 10L, 42, 100, true, java.time.LocalDateTime.now());
        when(videoCatalogPort.findByVideoId(10L)).thenReturn(Optional.of(accessInfo));
        when(videoProgressRepository.findByMemberIdAndVideoId(1L, 10L)).thenReturn(Optional.of(completed));

        service.handle(new MemberVideoCommand(1L, 10L));

        // 멱등 no-op: 재저장·이벤트 재발행·outbox 적재 없음, 메트릭은 성공(null)
        verify(videoProgressRepository, never()).save(any(VideoProgress.class));
        verify(eventPublisher, never()).publishEvent(any());
        verify(videoCompletionOutboxStore, never()).enqueue(any(), any(), any(), any());
        verify(metricRecorder).recordResult(LearningActivityAction.COMPLETE_VIDEO, null);
    }

    @Test
    void 시청_시간이_부족하면_VideoCompletedEvent를_발행하지_않는다() {
        VideoAccessInfo accessInfo = accessInfo();
        VideoProgress progress = new VideoProgress(100L, 1L, 20L, 10L, 42, 269, false, null);
        when(videoCatalogPort.findByVideoId(10L)).thenReturn(Optional.of(accessInfo));
        when(videoProgressRepository.findByMemberIdAndVideoId(1L, 10L)).thenReturn(Optional.of(progress));

        assertThatThrownBy(() -> service.handle(new MemberVideoCommand(1L, 10L)))
                .isInstanceOf(BusinessException.class);

        verify(eventPublisher, never()).publishEvent(any());
    }

    private VideoAccessInfo accessInfo() {
        return new VideoAccessInfo(
                10L,
                20L,
                "PUBLISHED",
                10000,
                false,
                "https://stream.example.com/video.m3u8",
                300
        );
    }
}
