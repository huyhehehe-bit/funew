package com.funews.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Du lieu nguoi dung tu sua ho so cua chinh minh. Khong cho doi quyen. */
public record ProfileRequest(
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Email @Size(max = 254) String email,
        @Size(max = 254) @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank") String username,
        LocalDate dateOfBirth) {
    public ProfileRequest(String fullName, String email) {
        this(fullName, email, null, null);
    }
}
