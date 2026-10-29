package com.funews.backend.dto;

import java.math.BigDecimal;

/**
 * Ket qua cham diem: diem cua chinh nguoi dung, kem so luot va diem trung binh
 * moi cua bai viet de frontend cap nhat ngay khong phai goi lai.
 *
 * score = null nghia la nguoi nay chua cham diem bai do.
 * ratingAvg do database tu tinh; tren H2 (test) cot nay khong phai cot tinh toan
 * nen luon null - dung assert vao no trong test, hay dung ratingCount.
 */
public record RatingResponse(Long articleId, Long accountId, Integer score,
                             long ratingCount, BigDecimal ratingAvg) {
}
