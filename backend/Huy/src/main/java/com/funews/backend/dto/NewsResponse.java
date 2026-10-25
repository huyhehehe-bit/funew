package com.funews.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.funews.backend.entity.ArticleStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Bai viet tra ve cho client. authorId va createdAt do server quyet dinh.
 *
 * ratingAvg do database tu tinh tu ratingSum / ratingCount, lam tron 2 chu so;
 * null khi bai chua co danh gia nao.
 * tags la danh sach ten the, lay qua bang noi article_tags.
 * version la so hieu ban ghi - client giu lai va gui kem khi sua bai, de server
 * phat hien duoc nguoi khac da sua bai trong luc minh dang mo form.
 */
public record NewsResponse(Long id, String title, String slug, String summary, String content,
                           String thumbnailUrl, String source, Long categoryId,
                           ArticleStatus status, Long authorId, Long reviewerId,
                           LocalDateTime submittedAt, LocalDateTime reviewedAt,
                           LocalDateTime scheduledPublishAt, LocalDateTime publishedAt,
                           long viewCount, long ratingCount, BigDecimal ratingAvg,
                           @JsonProperty("isFeatured") boolean isFeatured, List<String> tags,
                           LocalDateTime createdAt, LocalDateTime updatedAt, long version) {
}
