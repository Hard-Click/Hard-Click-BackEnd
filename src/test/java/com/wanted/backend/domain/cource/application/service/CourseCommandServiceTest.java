package com.wanted.backend.domain.cource.application.service;

import com.wanted.backend.domain.cource.application.command.ConfirmVideoUploadCommand;
import com.wanted.backend.domain.cource.application.command.RequestVideoUploadCommand;
import com.wanted.backend.domain.cource.application.port.CourseVideoCatalogSyncPort;
import com.wanted.backend.domain.cource.application.port.ThumbnailStoragePort;
import com.wanted.backend.domain.cource.application.port.VideoStoragePort;
import com.wanted.backend.domain.cource.domain.dto.CourseAuthorInfo;
import com.wanted.backend.domain.cource.domain.model.CourseStatus;
import com.wanted.backend.domain.cource.domain.model.Lesson;
import com.wanted.backend.domain.cource.domain.repository.CourseRepository;
import com.wanted.backend.domain.cource.domain.repository.LessonRepository;
import com.wanted.backend.domain.notification.domain.repository.NotificationRepository;
import com.wanted.backend.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseCommandServiceTest {

    private LessonRepository lessonRepository;
    private VideoStoragePort videoStoragePort;
    private CourseVideoCatalogSyncPort videoCatalogSyncPort;
    private CourseCommandService service;
    private ResourcelessTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        CourseRepository courseRepository = mock(CourseRepository.class);
        lessonRepository = mock(LessonRepository.class);
        videoStoragePort = mock(VideoStoragePort.class);
        ThumbnailStoragePort thumbnailStoragePort = mock(ThumbnailStoragePort.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        videoCatalogSyncPort = mock(CourseVideoCatalogSyncPort.class);
        Clock clock = Clock.fixed(Instant.parse("2026-06-27T00:00:00Z"), ZoneOffset.UTC);
        // 실제 DB 없이도 TransactionTemplate의 트랜잭션 동기화(afterCommit)가 동작하게 해주는
        // 리소스 없는 트랜잭션 매니저(테스트 더블) — 단위테스트 표준 패턴.
        transactionManager = new ResourcelessTransactionManager();

        service = new CourseCommandService(
                courseRepository,
                lessonRepository,
                videoStoragePort,
                thumbnailStoragePort,
                eventPublisher,
                transactionManager,
                clock,
                notificationRepository,
                videoCatalogSyncPort
        );
    }

    // 실제 DB 리소스 없이 트랜잭션 동기화(afterCommit 등) 생명주기만 정상 동작시키는 테스트 더블.
    private static class ResourcelessTransactionManager extends AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }

    private void confirmInTransaction(ConfirmVideoUploadCommand command) {
        // 실제로는 @Transactional 프록시가 메서드 전체를 트랜잭션으로 감싸므로,
        // 단위테스트에서도 동일하게 트랜잭션 안에서 호출해야 afterCommit 동기화 등록이 가능하다.
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                service.confirmVideoUpload(command));
    }

    @Test
    void 영상_업로드_presignedURL을_발급한다() {
        Long lessonId = 10L;
        Long authorId = 1L;
        when(lessonRepository.findCourseAuthorInfo(lessonId))
                .thenReturn(Optional.of(new CourseAuthorInfo(100L, authorId, CourseStatus.PUBLISHED)));
        when(videoStoragePort.generatePresignedPutUrl(lessonId, "lecture.mp4"))
                .thenReturn(new VideoStoragePort.PresignedUpload(
                        "https://s3.example.com/presigned", "videos/10_uuid.mp4", "video/mp4"));

        VideoStoragePort.PresignedUpload result = service.requestVideoUpload(
                new RequestVideoUploadCommand(lessonId, authorId, "lecture.mp4"));

        assertThat(result.presignedUrl()).isEqualTo("https://s3.example.com/presigned");
        assertThat(result.s3Key()).isEqualTo("videos/10_uuid.mp4");
        assertThat(result.contentType()).isEqualTo("video/mp4");
    }

    @Test
    void 영상_업로드_요청_시_강의_작성자가_아니면_거부하고_presignedURL을_발급하지_않는다() {
        Long lessonId = 10L;
        when(lessonRepository.findCourseAuthorInfo(lessonId))
                .thenReturn(Optional.of(new CourseAuthorInfo(100L, 1L, CourseStatus.PUBLISHED)));

        assertThatThrownBy(() -> service.requestVideoUpload(
                new RequestVideoUploadCommand(lessonId, 999L, "lecture.mp4")))
                .isInstanceOf(BusinessException.class);

        verify(videoStoragePort, never()).generatePresignedPutUrl(any(), any());
    }

    @Test
    void 영상_업로드_확인_시_s3Key가_레슨에_저장되고_카탈로그_동기화가_트리거된다() {
        Long lessonId = 10L;
        Long authorId = 1L;
        Long courseId = 100L;
        Lesson lesson = Lesson.create(5L, "1강", "설명", 0, null, Instant.now());
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(lessonRepository.findCourseAuthorInfo(lessonId))
                .thenReturn(Optional.of(new CourseAuthorInfo(courseId, authorId, CourseStatus.PUBLISHED)));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(videoStoragePort.getObjectSize("videos/10_uuid.mp4")).thenReturn(1024L);

        confirmInTransaction(new ConfirmVideoUploadCommand(lessonId, authorId, "videos/10_uuid.mp4", 120));

        ArgumentCaptor<Lesson> captor = ArgumentCaptor.forClass(Lesson.class);
        verify(lessonRepository).save(captor.capture());
        assertThat(captor.getValue().getS3Key()).isEqualTo("videos/10_uuid.mp4");
        verify(videoCatalogSyncPort).syncByCourse(courseId);
    }

    @Test
    void 영상_업로드_확인_시_다른_레슨용으로_발급된_키는_거부한다() {
        Long lessonId = 10L;
        Long authorId = 1L;
        Lesson lesson = Lesson.create(5L, "1강", "설명", 0, null, Instant.now());
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(lessonRepository.findCourseAuthorInfo(lessonId))
                .thenReturn(Optional.of(new CourseAuthorInfo(100L, authorId, CourseStatus.PUBLISHED)));

        assertThatThrownBy(() -> confirmInTransaction(
                new ConfirmVideoUploadCommand(lessonId, authorId, "videos/999_other.mp4", 120)))
                .isInstanceOf(BusinessException.class);

        verify(lessonRepository, never()).save(any());
    }

    @Test
    void 영상_업로드_확인_시_허용_크기를_초과하면_거부하고_S3_객체를_삭제한다() {
        Long lessonId = 10L;
        Long authorId = 1L;
        Lesson lesson = Lesson.create(5L, "1강", "설명", 0, null, Instant.now());
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(lessonRepository.findCourseAuthorInfo(lessonId))
                .thenReturn(Optional.of(new CourseAuthorInfo(100L, authorId, CourseStatus.PUBLISHED)));
        when(videoStoragePort.getObjectSize("videos/10_uuid.mp4")).thenReturn(2L * 1024 * 1024 * 1024); // 2GB

        assertThatThrownBy(() -> confirmInTransaction(
                new ConfirmVideoUploadCommand(lessonId, authorId, "videos/10_uuid.mp4", 120)))
                .isInstanceOf(BusinessException.class);

        verify(videoStoragePort).delete("videos/10_uuid.mp4");
        verify(lessonRepository, never()).save(any());
    }

    @Test
    void 영상_업로드_확인_시_카탈로그_동기화가_실패해도_예외가_전파되지_않는다() {
        Long lessonId = 10L;
        Long authorId = 1L;
        Long courseId = 100L;
        Lesson lesson = Lesson.create(5L, "1강", "설명", 0, null, Instant.now());
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(lessonRepository.findCourseAuthorInfo(lessonId))
                .thenReturn(Optional.of(new CourseAuthorInfo(courseId, authorId, CourseStatus.PUBLISHED)));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(videoStoragePort.getObjectSize("videos/10_uuid.mp4")).thenReturn(1024L);
        org.mockito.Mockito.doThrow(new RuntimeException("sync down"))
                .when(videoCatalogSyncPort).syncByCourse(courseId);

        assertThatCode(() -> confirmInTransaction(
                new ConfirmVideoUploadCommand(lessonId, authorId, "videos/10_uuid.mp4", 120)))
                .doesNotThrowAnyException();

        verify(lessonRepository).save(any(Lesson.class));
    }
}
