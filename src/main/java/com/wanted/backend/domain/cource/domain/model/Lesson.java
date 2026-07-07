package com.wanted.backend.domain.cource.domain.model;

import java.time.Instant;

public class Lesson {

    private Long id;
    private Long sectionId;
    private String title;
    private String description;
    private int orderIndex;
    private String videoUrl;
    private String s3Key;
    private Integer durationSeconds;
    private FileProcessingStatus fileProcessingStatus;
    private Instant createdAt;

    private Lesson() {}

    public static Lesson create(Long sectionId, String title, String description,
                                int orderIndex, Integer durationSeconds, Instant now) {
        Lesson lesson = new Lesson();
        lesson.sectionId = sectionId;
        lesson.title = title;
        lesson.description = description;
        lesson.orderIndex = orderIndex;
        lesson.durationSeconds = durationSeconds;
        lesson.createdAt = now;
        return lesson;
    }

    public static Lesson restore(Long id, Long sectionId, String title, String description,
                                 int orderIndex, String videoUrl, String s3Key, Integer durationSeconds,
                                 FileProcessingStatus fileProcessingStatus, Instant createdAt) {
        Lesson lesson = new Lesson();
        lesson.id = id;
        lesson.sectionId = sectionId;
        lesson.title = title;
        lesson.description = description;
        lesson.orderIndex = orderIndex;
        lesson.videoUrl = videoUrl;
        lesson.s3Key = s3Key;
        lesson.durationSeconds = durationSeconds;
        lesson.fileProcessingStatus = fileProcessingStatus;
        lesson.createdAt = createdAt;
        return lesson;
    }

    // 영상 파일 첨부 + PENDING 상태로 전이
    public void attachVideo(String videoUrl, String s3Key, Integer durationSeconds) {
        this.videoUrl = videoUrl;
        this.s3Key = s3Key;
        if (durationSeconds != null && durationSeconds > 0) {
            this.durationSeconds = durationSeconds;
        }
        this.fileProcessingStatus = FileProcessingStatus.PENDING;
    }

    // PROCESSING 상태로 전이
    public void startProcessing() {
        this.fileProcessingStatus = FileProcessingStatus.PROCESSING;
    }

    // COMPLETED 상태로 전이
    public void completeProcessing() {
        this.fileProcessingStatus = FileProcessingStatus.COMPLETED;
    }

    // FAILED 상태로 전이
    public void failProcessing() {
        this.fileProcessingStatus = FileProcessingStatus.FAILED;
    }

    public Long getId() { return id; }
    public Long getSectionId() { return sectionId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getOrderIndex() { return orderIndex; }
    public String getVideoUrl() { return videoUrl; }
    public String getS3Key() { return s3Key; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public FileProcessingStatus getFileProcessingStatus() { return fileProcessingStatus; }
    public Instant getCreatedAt() { return createdAt; }
}
