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
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Email @Size(max = 254) String email,
        @Size(max = 254) @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank") String username,
        @NotBlank @Size(min = 6, max = 72) String password,
        LocalDate dateOfBirth) {
}
