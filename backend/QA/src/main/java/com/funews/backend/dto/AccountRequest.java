package com.funews.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Du lieu tao hoac sua tai khoan. Password de trong khi sua nghia la giu mat khau cu.
 *
 * Doi so voi ban cu: name -> fullName, va role dang so 1/2 -> roleId tro ve bang
 * roles (1 ADMIN, 2 EDITOR, 3 CONTRIBUTOR, 4 READER).
 * isActive de null nghia la giu nguyen; tai khoan moi mac dinh dang hoat dong.
 */
public record AccountRequest(
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Email @Size(max = 254) String email,
        @Size(max = 72) String password,
        @NotNull @Positive Long roleId,
        @JsonProperty("isActive") Boolean isActive) {
}
