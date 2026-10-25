package com.funews.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.funews.backend.entity.ArticleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Du lieu tao hoac sua bai viet.
 *
 * Khong co authorId va createdAt: server tu gan tac gia la tai khoan dang dang
 * nhap va thoi diem la luc tao. Client gui kem cung bi bo qua. reviewer va cac
 * moc thoi gian cua luong duyet bai cung vay - chung do luong duyet sinh ra,
 * khong nhan tu client.
 *
 * Doi so voi ban cu: headline -> summary, status tu so 0/1 -> ArticleStatus
 * (chuoi DRAFT, PENDING_REVIEW, ...), tags tu mot chuoi "a, b, c" -> danh sach
 * ten the. Them slug, thumbnailUrl, scheduledPublishAt va isFeatured.
 *
 * slug de trong thi server tu sinh tu title.
 * scheduledPublishAt bat buoc co khi status = SCHEDULED.
 *
 * version: khi SUA bai, client gui lai dung con so da nhan ve tu lan doc truoc.
 * Service so sanh voi version dang nam trong database; lech nghia la nguoi khac
 * da sua bai nay trong luc do, phai tra 409 chu dung ghi de. Khi TAO bai moi
 * thi de trong.
 */
public record NewsRequest(
        @NotBlank @Size(max = 300) String title,
        @Size(max = 350) String slug,
        @Size(max = 1000) String summary,
        @Size(max = 1000000) String content,
        @Size(max = 500) String thumbnailUrl,
        @Size(max = 500) String source,
        @NotNull @Positive Long categoryId,
        List<@NotBlank @Size(max = 100) String> tags,
        @NotNull ArticleStatus status,
        LocalDateTime scheduledPublishAt,
        @JsonProperty("isFeatured") Boolean isFeatured,
        @PositiveOrZero Long version) {
}
