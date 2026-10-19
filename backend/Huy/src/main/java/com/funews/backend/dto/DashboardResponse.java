package com.funews.backend.dto;

import java.util.List;

/** So lieu tong quan cho man hinh Dashboard, kem toi da 5 bai viet moi nhat. */
public record DashboardResponse(long accountCount, long categoryCount, long newsCount,
                                long activeNewsCount, List<NewsResponse> recentNews) {
}
