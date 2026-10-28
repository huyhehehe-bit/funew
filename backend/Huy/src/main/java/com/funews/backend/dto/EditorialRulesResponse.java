package com.funews.backend.dto;

import java.time.LocalDateTime;

/**
 * Quy tac bien tap dang ap dung - SRS muc 3.2.
 *
 * Moi nguoi dang nhap deu doc duoc, vi tac gia can biet truoc quy tac truoc khi
 * nop bai. Nhung chi ADMIN moi sua duoc.
 */
public record EditorialRulesResponse(boolean autoPublishScheduled, boolean requireNoteOnApprove,
                                     int minSummaryLength, LocalDateTime updatedAt, Long updatedById) {
}
