package com.wanted.backend.domain.chat.presentation.response;

public record ChatErrorMessage(
        String errorCode,
        String message
) {}
