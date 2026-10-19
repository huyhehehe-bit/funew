package com.funews.backend.dto;

import java.time.LocalDate;

/** Bai viet tra ve cho client. createdById va createdDate do server quyet dinh. */
public record NewsResponse(Long id, String title, String headline, String content, String source,
                           Long categoryId, String tags, int status, Long createdById,
                           LocalDate createdDate) {
}
