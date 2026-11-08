package com.funews.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.funews.backend.entity.ArticleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.hibernate.validator.constraints.URL;

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
        @NotBlank(message = "Tiêu đề không được để trống")
        @Size(max = 300, message = "Tiêu đề tối đa 300 ký tự") String title,
        @Size(max = 350, message = "Slug tối đa 350 ký tự") String slug,
        @Size(max = 1000, message = "Tóm tắt tối đa 1000 ký tự") String summary,
        @Size(max = 1000000, message = "Nội dung quá dài") String content,
        @URL(message = "Ảnh đại diện phải là đường dẫn URL hợp lệ (bắt đầu bằng http:// hoặc https://)")
        @Size(max = 500, message = "Đường dẫn ảnh tối đa 500 ký tự") String thumbnailUrl,
        @URL(message = "Nguồn phải là đường dẫn URL hợp lệ (bắt đầu bằng http:// hoặc https://)")
        @Size(max = 500, message = "Nguồn tối đa 500 ký tự") String source,
        @NotNull(message = "Vui lòng chọn danh mục")
        @Positive(message = "Danh mục không hợp lệ") Long categoryId,
        List<@NotBlank(message = "Thẻ không được để trống")
             @Size(max = 100, message = "Mỗi thẻ tối đa 100 ký tự") String> tags,
        @NotNull(message = "Thiếu trạng thái bài viết") ArticleStatus status,
        @JsonProperty("isFeatured") Boolean isFeatured,
        @PositiveOrZero Long version) {
}
