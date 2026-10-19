package com.funews.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public final class ApiDtos {
    private ApiDtos() {
    }

    public record AccountResponse(Long id, String name, String email, int role) { }

    public record AccountLookup(Long id, String name) { }

    public record AccountRequest(
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Email @Size(max = 254) String email,
            @Size(max = 72) String password,
            @NotNull @Min(1) @Max(2) Integer role) { }

    public record ProfileRequest(
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Email @Size(max = 254) String email) { }

    public record CategoryResponse(Long id, String name, String description, Long parentId, int isActive) { }

    public record CategoryRequest(
            @NotBlank @Size(max = 150) String name,
            @Size(max = 2000) String description,
            @PositiveOrZero Long parentId,
            @NotNull @Min(0) @Max(1) Integer isActive) { }

    public record NewsResponse(Long id, String title, String headline, String content, String source,
                               Long categoryId, String tags, int status, Long createdById,
                               LocalDate createdDate) { }

    public record NewsRequest(
            @NotBlank @Size(max = 300) String title,
            @Size(max = 1000) String headline,
            @Size(max = 1000000) String content,
            @Size(max = 500) String source,
            @NotNull @Positive Long categoryId,
            @Size(max = 1000) String tags,
            @NotNull @Min(0) @Max(1) Integer status) { }

    public record DashboardResponse(long accountCount, long categoryCount, long newsCount,
                                    long activeNewsCount, List<NewsResponse> recentNews) { }
}
