package com.funews.backend.dto;

import java.util.List;

/**
 * So lieu tong quan cho man hinh Dashboard, kem toi da 5 bai viet moi nhat.
 *
 * activeNewsCount cu doi thanh hai so cua luong bien tap: publishedCount la so
 * bai dang cong khai, pendingReviewCount la so bai dang cho duyet.
 */
public record DashboardResponse(long accountCount, long categoryCount, long newsCount,
                                long publishedCount, long pendingReviewCount,
                                List<NewsResponse> recentNews) {
}
