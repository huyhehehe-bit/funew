package com.funews.backend.controller;

import com.funews.backend.dto.RatingRequest;
import com.funews.backend.dto.RatingResponse;
import com.funews.backend.exception.ApiException;
import com.funews.backend.security.AccountPrincipal;
import com.funews.backend.service.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cham diem bai viet.
 *
 * Ca ba endpoint deu PHAI dang nhap - day la khac biet duy nhat giua khach vang
 * lai va doc gia da dang ky. Diem trung binh thi ai cung xem duoc vi no nam san
 * trong ratingAvg cua moi bai viet.
 *
 * Duong dan 3 doan /api/news/{id}/rating khong khop rule permitAll
 * "/api/news/*" (chi 2 doan) nen tu dong roi vao anyRequest().authenticated().
 */
@Tag(name = "7. Ratings", description = "Cham diem 1-5 sao, chi danh cho tai khoan da dang nhap. Phu trach: Huy")
@RestController
@RequestMapping("/api/news/{id}/rating")
@SecurityRequirement(name = "bearerAuth")
public class RatingController {
    private final RatingService service;

    public RatingController(RatingService service) {
        this.service = service;
    }

    @Operation(summary = "Diem cua chinh minh cho bai nay - null la chua cham")
    @GetMapping
    public RatingResponse myRating(@PathVariable Long id, Authentication authentication) {
        return service.myRating(id, actor(authentication));
    }

    @Operation(summary = "Cham hoac doi diem - chi bai dang cong khai")
    @PutMapping
    public RatingResponse rate(@PathVariable Long id, @Valid @RequestBody RatingRequest request,
                               Authentication authentication) {
        return service.rate(id, request, actor(authentication));
    }

    @Operation(summary = "Go diem cua chinh minh")
    @DeleteMapping
    public RatingResponse removeRating(@PathVariable Long id, Authentication authentication) {
        return service.removeRating(id, actor(authentication));
    }

    private AccountPrincipal actor(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal) {
            return principal;
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, "Vui long dang nhap de cham diem.");
    }
}
