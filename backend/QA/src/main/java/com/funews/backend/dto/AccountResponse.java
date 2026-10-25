package com.funews.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Tai khoan tra ve cho client. Khong bao gio chua mat khau hay hash.
 *
 * roleCode la ma vai tro doc tu bang roles (ADMIN, EDITOR, CONTRIBUTOR, READER)
 * de frontend khong phai tu doan y nghia cua roleId.
 */
public record AccountResponse(Long id, String fullName, String email, Long roleId, String roleCode,
                              @JsonProperty("isActive") boolean isActive) {
}
