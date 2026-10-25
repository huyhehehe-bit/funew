package com.funews.backend.dto;

import java.math.BigDecimal;

/**
 * Ket qua sau khi cham diem: diem cua chinh nguoi dung, kem so luot va diem
 * trung binh moi cua bai viet de frontend cap nhat ngay khong phai goi lai.
 */
public record RatingResponse(Long articleId, Long accountId, int score,
                             long ratingCount, BigDecimal ratingAvg) {
}
