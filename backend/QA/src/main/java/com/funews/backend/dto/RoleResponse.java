package com.funews.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Vai tro tra ve cho client, dung cho o chon quyen khi tao hoac sua tai khoan. */
public record RoleResponse(Long id, String code, String name, String description,
                           @JsonProperty("isActive") boolean isActive) {
}
