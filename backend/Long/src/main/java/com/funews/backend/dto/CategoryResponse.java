package com.funews.backend.dto;

/** Danh muc tra ve cho client. parentId = 0 nghia la khong co danh muc cha. */
public record CategoryResponse(Long id, String name, String description, Long parentId, int isActive) {
}
