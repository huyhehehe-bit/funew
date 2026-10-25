package com.funews.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.funews.backend.entity.NotificationType;
import java.time.LocalDateTime;

/**
 * Mot thong bao trong hop thu cua nguoi dung dang dang nhap.
 *
 * Khong co accountId: hop thu luon la cua chinh nguoi goi API.
 * articleId null nghia la thong bao khong gan voi bai nao.
 */
public record NotificationResponse(Long id, Long articleId, NotificationType type, String message,
                                   @JsonProperty("isRead") boolean isRead, LocalDateTime readAt,
                                   LocalDateTime createdAt) {
}
