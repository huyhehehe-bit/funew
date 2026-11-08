package com.funews.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Khach tu dang ky tai khoan doc gia.
 *
 * KHONG CO roleId: vai tro do server ep cung la READER. Neu de client chon thi
 * ai cung tu dang ky lam EDITOR roi tu duyet bai cua minh - thung NFR-ED-003.
 * Khach gui kem roleId cung bi bo qua chu khong phai bi tu choi.
 *
 * username de trong thi lay email. Mat khau toi thieu 6 ky tu: day la tai khoan
 * nguoi la tu tao, chat hon mot chut so voi tai khoan do Administrator cap.
 */
public record RegisterRequest(
        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 150, message = "Họ tên tối đa 150 ký tự") String fullName,
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        @Size(max = 254, message = "Email tối đa 254 ký tự") String email,
        @Size(max = 254, message = "Tên đăng nhập tối đa 254 ký tự")
        @Pattern(regexp = "(?s).*\\S.*", message = "Tên đăng nhập không được để trống") String username,
        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 6, max = 72, message = "Mật khẩu phải từ 6 đến 72 ký tự") String password,
        LocalDate dateOfBirth) {
}
