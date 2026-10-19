package com.funews.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Du lieu nguoi dung tu sua ho so cua chinh minh. Khong cho doi quyen. */
public record ProfileRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Email @Size(max = 254) String email) {
}
