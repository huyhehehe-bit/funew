package com.funews.backend.dto;

import com.funews.backend.entity.ArticleStatus;
import java.time.LocalDateTime;

/**
 * Mot dong nhat ky kiem toan cua bai viet - FR-ED-004.
 *
 * fromStatus null o dong dau tien. note la ly do tra bai, bat buoc co khi
 * toStatus = CHANGES_REQUESTED.
 */
public record ArticleAuditLogResponse(Long id, Long articleId, ArticleStatus fromStatus,
                                      ArticleStatus toStatus, String action, String note,
                                      Long actorId, LocalDateTime createdAt) {
}
