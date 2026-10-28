package com.funews.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.funews.backend.entity.ArticleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Du lieu tao hoac sua bai viet.
 *
 * Khong co authorId va createdAt: server tu gan tac gia la tai khoan dang dang
 * nhap va thoi diem la luc tao. Client gui kem cung bi bo qua. reviewer va cac
 * moc thoi gian cua luong duyet bai cung vay.
 *
 * status chi nhan DRAFT hoac PENDING_REVIEW. Moi trang thai sau do la viec cua
 * luong duyet bai (cac endpoint submit / request-changes / approve / publish /
 * archive), nen khong ai PUT thang mot bai len PUBLISHED duoc - FR-ED-001.
 * Khi SUA bai, status phai bang dung trang thai hien tai cua bai.
 *
 * Khong co scheduledPublishAt: gio hen dang do bien tap vien dat luc duyet bai,
 * nam trong ReviewRequest - FR-ED-003.
 *
 * isFeatured chi bien tap vien va quan tri vien dat duoc; tac gia gui len thi
 * bi bo qua.
 *
 * slug de trong thi server tu sinh tu title, va tu them -2, -3 neu trung.
 *
 * version: khi SUA bai, gui lai dung con so da nhan ve tu lan doc truoc. Lech
 * nghia la nguoi khac vua sua bai nay, server tra 409 chu khong ghi de. Khi TAO
 * bai moi thi de trong.
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
        @JsonProperty("isFeatured") Boolean isFeatured,
        @PositiveOrZero Long version) {
}
