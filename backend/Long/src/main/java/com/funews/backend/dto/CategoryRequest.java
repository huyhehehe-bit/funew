package com.funews.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Du lieu tao hoac sua danh muc. */
public record CategoryRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 2000) String description,
        @PositiveOrZero Long parentId,
        @NotNull @Min(0) @Max(1) Integer isActive) {
}
