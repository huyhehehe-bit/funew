package com.funews.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Admin sua quy tac bien tap. Ca ba truong deu bat buoc gui: man hinh cau hinh
 * luon gui ve day du trang thai hien tai, khong sua le tung o.
 *
 * minSummaryLength toi da 1000 vi cot summary chi chua duoc 1000 ky tu - dat
 * lon hon thi khong bai nao nop duoc nua.
 */
public record EditorialRulesRequest(@NotNull Boolean autoPublishScheduled,
                                    @NotNull Boolean requireNoteOnApprove,
                                    @NotNull @Min(0) @Max(1000) Integer minSummaryLength) {
}
