package com.funews.backend.controller;

import com.funews.backend.dto.ArticleAuditLogResponse;
import com.funews.backend.dto.DashboardResponse;
import com.funews.backend.dto.NewsRequest;
import com.funews.backend.dto.NewsResponse;
import com.funews.backend.dto.ReviewRequest;
import com.funews.backend.exception.ApiException;
import com.funews.backend.security.AccountPrincipal;
import com.funews.backend.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Huy - bai viet, luong duyet bai va dashboard.
 *
 * Phan quyen theo URL khai trong SecurityConfig cua QA; o day chi lay ra tai
 * khoan dang dang nhap roi chuyen cho NewsService, vi chi tang service moi biet
 * ai la tac gia cua bai nao.
 *
 * Moi hanh dong doi trang thai la mot endpoint rieng, khong phai mot truong
 * trong body - nho the URL phan quyen duoc va khong ai doi trang thai bang cach
 * gui kem mot chu vao JSON.
 */
@Tag(name = "4. News", description = "Bai viet, luong duyet bai va dashboard. Phu trach: Huy")
@RestController
@RequestMapping("/api")
public class NewsController {
    private final NewsService service;

    public NewsController(NewsService service) {
        this.service = service;
    }

    @Operation(summary = "Danh sach bai viet - tra ve khac nhau tuy vai tro")
    @GetMapping("/news")
    public List<NewsResponse> news(Authentication authentication) {
        return service.listNews(viewer(authentication));
    }

    @Operation(summary = "Hang doi duyet bai - EDITOR va ADMIN")
    @GetMapping("/news/review-queue")
    @SecurityRequirement(name = "bearerAuth")
    public List<NewsResponse> reviewQueue() {
        return service.reviewQueue();
    }

    @Operation(summary = "Xem mot bai viet")
    @GetMapping("/news/{id}")
    public NewsResponse article(@PathVariable Long id, Authentication authentication) {
        return service.getNews(id, viewer(authentication));
    }

    @Operation(summary = "Nhat ky doi trang thai cua bai viet - FR-ED-004")
    @GetMapping("/news/{id}/audit")
    public List<ArticleAuditLogResponse> audit(@PathVariable Long id, Authentication authentication) {
        return service.auditTrail(id, viewer(authentication));
    }

    @Operation(summary = "Tao bai viet - DRAFT hoac nop luon PENDING_REVIEW")
    @PostMapping("/news")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<NewsResponse> createNews(@Valid @RequestBody NewsRequest request,
                                                   Authentication authentication) {
        NewsResponse created = service.createNews(request, actor(authentication));
        return ResponseEntity.created(URI.create("/api/news/" + created.id())).body(created);
    }

    @Operation(summary = "Sua bai viet - khong doi duoc trang thai")
    @PutMapping("/news/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public NewsResponse updateNews(@PathVariable Long id, @Valid @RequestBody NewsRequest request,
                                   Authentication authentication) {
        return service.updateNews(id, request, actor(authentication));
    }

    @Operation(summary = "Xoa bai viet - xoa mem, du lieu van con trong database")
    @DeleteMapping("/news/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deleteNews(@PathVariable Long id, Authentication authentication) {
        service.deleteNews(id, actor(authentication));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Nop bai cho bien tap - tac gia")
    @PostMapping("/news/{id}/submit")
    @SecurityRequirement(name = "bearerAuth")
    public NewsResponse submit(@PathVariable Long id, Authentication authentication) {
        return service.submit(id, actor(authentication));
    }

    @Operation(summary = "Tra bai lai kem ly do - bat buoc co note, FR-ED-002")
    @PostMapping("/news/{id}/request-changes")
    @SecurityRequirement(name = "bearerAuth")
    public NewsResponse requestChanges(@PathVariable Long id, @Valid @RequestBody ReviewRequest request,
                                       Authentication authentication) {
        return service.requestChanges(id, request, actor(authentication));
    }

    @Operation(summary = "Duyet bai - co scheduledPublishAt thi hen gio, khong co thi dang ngay")
    @PostMapping("/news/{id}/approve")
    @SecurityRequirement(name = "bearerAuth")
    public NewsResponse approve(@PathVariable Long id,
                                @Valid @RequestBody(required = false) ReviewRequest request,
                                Authentication authentication) {
        return service.approve(id, request, actor(authentication));
    }

    @Operation(summary = "Dang som mot bai dang cho toi gio hen")
    @PostMapping("/news/{id}/publish")
    @SecurityRequirement(name = "bearerAuth")
    public NewsResponse publish(@PathVariable Long id, Authentication authentication) {
        return service.publish(id, actor(authentication));
    }

    @Operation(summary = "Go bai khoi trang cong khai, van giu trong database")
    @PostMapping("/news/{id}/archive")
    @SecurityRequirement(name = "bearerAuth")
    public NewsResponse archive(@PathVariable Long id, Authentication authentication) {
        return service.archive(id, actor(authentication));
    }

    @Operation(summary = "Thong ke tong quan va 5 bai moi nhat ma nguoi dang nhap duoc thay")
    @GetMapping("/dashboard")
    @SecurityRequirement(name = "bearerAuth")
    public DashboardResponse dashboard(Authentication authentication) {
        return service.dashboard(viewer(authentication));
    }

    /** Khach vang lai: tra ve null chu khong nem loi, de cac endpoint doc cong khai van chay. */
    private AccountPrincipal viewer(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal
                ? principal : null;
    }

    private AccountPrincipal actor(Authentication authentication) {
        AccountPrincipal principal = viewer(authentication);
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Vui long dang nhap.");
        }
        return principal;
    }
}
