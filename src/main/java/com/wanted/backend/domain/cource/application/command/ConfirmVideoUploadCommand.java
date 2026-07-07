package com.wanted.backend.domain.cource.application.command;

public record ConfirmVideoUploadCommand(Long lessonId, Long requesterId, String s3Key, Integer durationSeconds) {}
