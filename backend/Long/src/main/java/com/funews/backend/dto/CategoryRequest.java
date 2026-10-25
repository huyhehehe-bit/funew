package com.funews.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Du lieu tao hoac sua danh muc.
 *
 * slug de trong thi server tu sinh tu name. displayOrder de trong thi coi la 0.
 * isActive doi tu so 0/1 sang boolean vi cot trong database la BIT.
 */
public record CategoryRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 180) String slug,
        @Size(max = 2000) String description,
        @PositiveOrZero Long parentId,
        @PositiveOrZero Integer displayOrder,
        @NotNull @JsonProperty("isActive") Boolean isActive) {
}
