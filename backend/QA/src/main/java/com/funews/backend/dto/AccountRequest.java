package com.funews.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Du lieu tao hoac sua tai khoan. Password de trong khi sua nghia la giu mat khau cu. */
public record AccountRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Email @Size(max = 254) String email,
        @Size(max = 72) String password,
        @NotNull @Min(1) @Max(2) Integer role) {
}
