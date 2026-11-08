package com.funews.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CategoryRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 180) String slug,
        @Size(max = 2000) String description,
        @PositiveOrZero Long parentId,
        @PositiveOrZero Integer displayOrder,
        @NotNull @JsonProperty("isActive") Boolean isActive,
        LocalDate date) {
}