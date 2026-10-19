package com.funews.backend.controller;

import com.funews.backend.dto.DashboardResponse;
import com.funews.backend.dto.NewsRequest;
import com.funews.backend.dto.NewsResponse;
import com.funews.backend.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
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

/** Huy - bai viet va dashboard. Phan quyen da khai trong SecurityConfig cua QA. */
@Tag(name = "4. News", description = "Quan ly bai viet va thong ke dashboard. Phu trach: Huy")
@RestController
@RequestMapping("/api")
public class NewsController {
    private final NewsService service;

    public NewsController(NewsService service) {
        this.service = service;
    }

    @Operation(summary = "Danh sach bai viet")
    @GetMapping("/news")
    public List<NewsResponse> news() {
        return service.listNews();
    }

    @Operation(summary = "Xem mot bai viet")
    @GetMapping("/news/{id}")
    public NewsResponse article(@PathVariable Long id) {
        return service.getNews(id);
    }

    @Operation(summary = "Tao bai viet - server tu gan tac gia va ngay tao")
    @PostMapping("/news")
    public ResponseEntity<NewsResponse> createNews(@Valid @RequestBody NewsRequest request,
                                                   Authentication authentication) {
        // authentication.getName() la ten cua tai khoan dang dang nhap, do SecurityConfig dat vao.
        NewsResponse created = service.createNews(request, authentication.getName());
        return ResponseEntity.created(URI.create("/api/news/" + created.id())).body(created);
    }

    @Operation(summary = "Sua bai viet - giu nguyen tac gia va ngay tao ban dau")
    @PutMapping("/news/{id}")
    public NewsResponse updateNews(@PathVariable Long id, @Valid @RequestBody NewsRequest request) {
        return service.updateNews(id, request);
    }

    @Operation(summary = "Xoa bai viet")
    @DeleteMapping("/news/{id}")
    public ResponseEntity<Void> deleteNews(@PathVariable Long id) {
        service.deleteNews(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Thong ke tong quan va 5 bai viet moi nhat")
    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return service.dashboard();
    }
}
