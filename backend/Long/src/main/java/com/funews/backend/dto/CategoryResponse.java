package com.funews.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Danh muc tra ve cho client. parentId = 0 nghia la khong co danh muc cha. */
public record CategoryResponse(Long id, String name, String slug, String description, Long parentId,
                               int displayOrder, @JsonProperty("isActive") boolean isActive) {
}
