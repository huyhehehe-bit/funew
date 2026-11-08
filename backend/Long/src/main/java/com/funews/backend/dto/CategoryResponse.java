package com.funews.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

public record CategoryResponse(
        Long id,
        String name,
        String slug,
        String description,
        Long parentId,
        int displayOrder,
        @JsonProperty("isActive") boolean isActive,
        LocalDate date) {
}