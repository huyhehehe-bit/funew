package com.funews.backend.dto;

/** Tai khoan tra ve cho client. Khong bao gio chua mat khau hay hash. */
public record AccountResponse(Long id, String name, String email, int role) {
}
