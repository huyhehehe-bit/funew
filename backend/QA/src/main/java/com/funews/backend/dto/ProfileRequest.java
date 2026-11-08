package com.funews.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Du lieu nguoi dung tu sua ho so cua chinh minh. Khong cho doi quyen. */
public record ProfileRequest(
        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 150, message = "Họ tên tối đa 150 ký tự") String fullName,
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        @Size(max = 254, message = "Email tối đa 254 ký tự") String email,
        @Size(max = 254, message = "Tên đăng nhập tối đa 254 ký tự")
        @Pattern(regexp = "(?s).*\\S.*", message = "Tên đăng nhập không được để trống") String username,
        LocalDate dateOfBirth) {
    public ProfileRequest(String fullName, String email) {
        this(fullName, email, null, null);
    }
}
