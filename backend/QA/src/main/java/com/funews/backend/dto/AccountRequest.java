package com.funews.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Du lieu tao hoac sua tai khoan. Password de trong khi sua nghia la giu mat khau cu.
 *
 * Doi so voi ban cu: name -> fullName, va role dang so 1/2 -> roleId tro ve bang
 * roles (1 ADMIN, 2 EDITOR, 3 CONTRIBUTOR, 4 READER).
 * isActive de null nghia la giu nguyen; tai khoan moi mac dinh dang hoat dong.
 * Username de null khi tao se lay email; khi sua thi giu nguyen.
 * dateOfBirth de null khi sua nghia la giu nguyen. status 0/1 phai khop isActive neu gui ca hai.
 */
public record AccountRequest(
        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 150, message = "Họ tên tối đa 150 ký tự") String fullName,
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        @Size(max = 254, message = "Email tối đa 254 ký tự") String email,
        @Size(max = 72, message = "Mật khẩu tối đa 72 ký tự") String password,
        @NotNull(message = "Vui lòng chọn vai trò")
        @Positive(message = "Vai trò không hợp lệ") Long roleId,
        @JsonProperty("isActive") Boolean isActive,
        @Size(max = 254, message = "Tên đăng nhập tối đa 254 ký tự")
        @Pattern(regexp = "(?s).*\\S.*", message = "Tên đăng nhập không được để trống") String username,
        LocalDate dateOfBirth,
        @Min(0) @Max(1) Integer status) {
    public AccountRequest(String fullName, String email, String password, Long roleId, Boolean isActive) {
        this(fullName, email, password, roleId, isActive, null, null, null);
    }
}
