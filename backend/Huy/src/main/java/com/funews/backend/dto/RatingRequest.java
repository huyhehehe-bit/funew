package com.funews.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Doc gia cham diem mot bai viet.
 *
 * Khong co accountId: server lay tai khoan dang dang nhap. Moi nguoi chi co mot
 * diem cho mot bai - gui lai la sua diem cu, khong them dong moi.
 */
public record RatingRequest(@NotNull @Min(1) @Max(5) Integer score) {
}
