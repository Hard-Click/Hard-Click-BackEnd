package com.wanted.backend.domain.cource.application.service;

import com.wanted.backend.domain.cource.application.command.ChangeCourseStatusCommand;
import com.wanted.backend.domain.cource.application.command.ConfirmVideoUploadCommand;
import com.wanted.backend.domain.cource.application.command.CreateCourseCommand;
import com.wanted.backend.domain.cource.application.command.RequestVideoUploadCommand;
import com.wanted.backend.domain.cource.application.command.UpdateCourseCommand;
import com.wanted.backend.domain.cource.application.command.UploadCourseThumbnailCommand;
import com.wanted.backend.domain.cource.application.port.CourseVideoCatalogSyncPort;
import com.wanted.backend.domain.cource.application.port.ThumbnailStoragePort;
import com.wanted.backend.domain.cource.application.port.VideoStoragePort;
import com.wanted.backend.domain.cource.application.usecase.CourseCommandUseCase;
import com.wanted.backend.domain.cource.domain.dto.CourseAuthorInfo;
import com.wanted.backend.domain.cource.domain.event.CourseCreatedEvent;
import com.wanted.backend.domain.cource.domain.model.Course;
import com.wanted.backend.domain.cource.domain.model.CourseSection;
import com.wanted.backend.domain.cource.domain.model.CourseStatus;
import com.wanted.backend.domain.cource.domain.model.Lesson;
import com.wanted.backend.domain.cource.domain.repository.CourseRepository;
import com.wanted.backend.domain.cource.domain.repository.LessonRepository;
import com.wanted.backend.domain.notification.domain.repository.NotificationRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CourseCommandService implements CourseCommandUseCase {

    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final VideoStoragePort videoStoragePort;
    private final ThumbnailStoragePort thumbnailStoragePort;
    private final ApplicationEventPublisher eventPublisher;
    private final PlatformTransactionManager transactionManager;
    private final Clock clock;
    private final NotificationRepository notificationRepository;
    private final CourseVideoCatalogSyncPort videoCatalogSyncPort;

    @Override
    public Long create(CreateCourseCommand command) {
        Instant now = clock.instant();

        List<CourseSection> sections = command.sections().stream()
                .map(s -> {
                    List<Lesson> lessons = s.lessons().stream()
                            .map(l -> Lesson.create(null, l.title(), l.description(), l.orderIndex(), l.durationSeconds(), now))
                            .toList();
                    return CourseSection.create(s.title(), s.orderIndex(), lessons);
                })
                .toList();

        Course course = Course.create(
                command.authorId(),
                command.title(),
                command.subject(),
                command.description(),
                command.thumbnailUrl(),
                command.priceType(),
                command.price(),
                sections,
                now,
                command.learningObjectives(),
                command.targetAudience(),
                command.techTags(),
                command.level()
        );

        Course saved = courseRepository.save(course);
        saved.pullDomainEvents().forEach(eventPublisher::publishEvent);

        eventPublisher.publishEvent(CourseCreatedEvent.of(
                saved.getId(), command.authorId(), saved.getTitle()));

        // 커밋 후 재생 스키마(course_curriculum/video)로 미러링
        registerVideoCatalogSync(saved.getId());

        return saved.getId();
    }

    @Override
    public void update(UpdateCourseCommand command) {
        Course course = courseRepository.findById(command.courseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        if (course.isDeleted()) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }

        if (!course.getAuthorId().equals(command.requesterId())) {
            throw new BusinessException(ErrorCode.COURSE_ACCESS_DENIED);
        }

        List<CourseSection> newSections = command.sections().stream()
                .map(sc -> {
                    List<Lesson> lessons = sc.lessons().stream()
                            .map(lc -> lc.id() != null
                                    ? Lesson.restore(lc.id(), null, lc.title(), lc.description(),
                                                     lc.orderIndex(), null, null, null, null, null)
                                    : Lesson.create(null, lc.title(), lc.description(),
                                                    lc.orderIndex(), lc.durationSeconds(), clock.instant()))
                            .toList();
                    return sc.id() != null
                            ? CourseSection.restore(sc.id(), sc.title(), sc.orderIndex(), lessons)
                            : CourseSection.create(sc.title(), sc.orderIndex(), lessons);
                })
                .toList();

        course.update(command.title(), command.subject(), command.description(),
                command.thumbnailUrl(), command.priceType(), command.price(), newSections,
                command.learningObjectives(), command.targetAudience(),
                command.techTags(), command.level());

        courseRepository.save(course);

        // 섹션/레슨 변경을 재생 스키마(course_curriculum/video)에 반영
        registerVideoCatalogSync(command.courseId());
    }

    @Override
    public void delete(Long courseId, Long requesterId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        if (!course.getAuthorId().equals(requesterId)) {
            throw new BusinessException(ErrorCode.COURSE_ACCESS_DENIED);
        }

        course.softDelete();
        courseRepository.save(course);
        notificationRepository.deleteByRedirectUrlStartingWith("/admin/courses/manage/" + courseId);
    }

    @Override
    public void changeStatus(ChangeCourseStatusCommand command) {
        Course course = courseRepository.findById(command.courseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        if (course.isDeleted()) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }

        if (!course.getAuthorId().equals(command.requesterId())) {
            throw new BusinessException(ErrorCode.COURSE_ACCESS_DENIED);
        }

        if (command.targetStatus() == CourseStatus.PUBLISHED) {
            course.publish();
        } else {
            course.unpublish();
        }

        courseRepository.save(course);
    }

    // S3VideoStorageAdapter.generatePresignedPutUrl()이 발급하는 키 prefix와 맞춰, confirm 시
    // 임의의(다른 레슨/외부) S3 키가 첨부되는 것을 막는다.
    private static final String VIDEO_KEY_PREFIX = "videos/";
    private static final long MAX_VIDEO_BYTES = 1024L * 1024 * 1024; // 1GB — application.yaml 멀티파트 한도와 동일
    private static final int VIDEO_CATALOG_SYNC_MAX_ATTEMPTS = 3;
    private static final long VIDEO_CATALOG_SYNC_RETRY_DELAY_MS = 200L;

    @Override
    @Transactional(readOnly = true)
    public VideoStoragePort.PresignedUpload requestVideoUpload(RequestVideoUploadCommand command) {
        CourseAuthorInfo courseInfo = lessonRepository.findCourseAuthorInfo(command.lessonId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LESSON_NOT_FOUND));
        if (courseInfo.isDeleted()) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }
        if (!courseInfo.authorId().equals(command.requesterId())) {
            throw new BusinessException(ErrorCode.COURSE_ACCESS_DENIED);
        }

        return videoStoragePort.generatePresignedPutUrl(command.lessonId(), command.originalFilename());
    }

    @Override
    public void confirmVideoUpload(ConfirmVideoUploadCommand command) {
        Lesson lesson = lessonRepository.findById(command.lessonId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LESSON_NOT_FOUND));

        CourseAuthorInfo courseInfo = lessonRepository.findCourseAuthorInfo(command.lessonId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LESSON_NOT_FOUND));
        if (courseInfo.isDeleted()) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }
        if (!courseInfo.authorId().equals(command.requesterId())) {
            throw new BusinessException(ErrorCode.COURSE_ACCESS_DENIED);
        }

        // 이 레슨용으로 발급된 키가 맞는지 prefix로 확인 — 임의의 s3Key를 붙이는 것을 차단한다.
        String expectedPrefix = VIDEO_KEY_PREFIX + command.lessonId() + "_";
        if (!command.s3Key().startsWith(expectedPrefix)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // presigned PUT 자체는 크기를 제한할 수 없으므로, 실제로 업로드된 객체 크기를 confirm 시점에 검증한다.
        long size = videoStoragePort.getObjectSize(command.s3Key());
        if (size > MAX_VIDEO_BYTES) {
            videoStoragePort.delete(command.s3Key());
            throw new BusinessException(ErrorCode.VIDEO_FILE_SIZE_EXCEEDED);
        }

        // video_url에는 s3Key를 저장하고 COMPLETED로 전이 — presigned URL 직접 업로드는 별도 처리 단계 없음
        lesson.attachVideo(command.s3Key(), command.s3Key(), command.durationSeconds());
        lesson.completeProcessing();
        lessonRepository.save(lesson);

        registerVideoCatalogSync(courseInfo.courseId());
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String uploadCourseThumbnail(UploadCourseThumbnailCommand command) {
        Course course = courseRepository.findById(command.courseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
        if (course.isDeleted()) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }
        if (!course.getAuthorId().equals(command.requesterId())) {
            throw new BusinessException(ErrorCode.COURSE_ACCESS_DENIED);
        }

        ThumbnailStoragePort.StoredThumbnail stored = thumbnailStoragePort.store(
                command.courseId(), command.originalFilename(), command.imageData());

        String oldKey = course.getThumbnailUrl();
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                course.updateThumbnail(stored.key());
                courseRepository.save(course);
            });
        } catch (RuntimeException e) {
            thumbnailStoragePort.delete(stored.key());
            throw e;
        }

        // 이전 썸네일 S3 객체 정리 (http(s) URL이면 외부 이미지이므로 삭제 안 함)
        if (oldKey != null && !oldKey.startsWith("http")) {
            thumbnailStoragePort.delete(oldKey);
        }

        return stored.presignedUrl();
    }

    // 커밋 이후 작성 스키마(course_section/lesson)를 재생 스키마(course_curriculum/video)로 미러링한다.
    // 실패 시 재생 API가 VIDEO_NOT_FOUND를 반환하게 되므로, 일시적 오류를 흡수하기 위해 짧게 재시도하고
    // 최종 실패는 ERROR로 남겨 수동 재동기화가 필요함을 알 수 있게 한다.
    private void registerVideoCatalogSync(Long courseId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // videoCatalogSyncPort.syncByCourse()가 REQUIRES_NEW로 자체 트랜잭션 경계를
                // 갖고 있으므로 여기서는 별도 TransactionTemplate으로 한 번 더 감싸지 않는다.
                for (int attempt = 1; attempt <= VIDEO_CATALOG_SYNC_MAX_ATTEMPTS; attempt++) {
                    try {
                        videoCatalogSyncPort.syncByCourse(courseId);
                        return;
                    } catch (Exception e) {
                        if (attempt == VIDEO_CATALOG_SYNC_MAX_ATTEMPTS) {
                            log.error("video catalog 미러링 실패(courseId={}, attempts={}) — 재생 API에서 영상이 조회되지 않을 수 있어 수동 재동기화가 필요함",
                                    courseId, attempt, e);
                        } else {
                            log.warn("video catalog 미러링 재시도(courseId={}, attempt={}/{})",
                                    courseId, attempt, VIDEO_CATALOG_SYNC_MAX_ATTEMPTS, e);
                            if (!sleepBeforeRetry()) {
                                return;
                            }
                        }
                    }
                }
            }
        });
    }

    // 인터럽트(배포/종료 등) 시 false를 반환해 재시도 루프를 즉시 중단시킨다 — 불필요한 DB 부하와 종료 지연 방지.
    private boolean sleepBeforeRetry() {
        try {
            Thread.sleep(VIDEO_CATALOG_SYNC_RETRY_DELAY_MS);
            return true;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
