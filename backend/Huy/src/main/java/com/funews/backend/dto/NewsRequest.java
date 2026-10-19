package com.funews.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Du lieu tao hoac sua bai viet.
 *
 * Khong co createdById va createdDate: server tu gan tac gia la tai khoan dang
 * dang nhap va ngay la hom nay. Client gui kem cung bi bo qua.
 */
public record NewsRequest(
        @NotBlank @Size(max = 300) String title,
        @Size(max = 1000) String headline,
        @Size(max = 1000000) String content,
        @Size(max = 500) String source,
        @NotNull @Positive Long categoryId,
        @Size(max = 1000) String tags,
        @NotNull @Min(0) @Max(1) Integer status) {
}
