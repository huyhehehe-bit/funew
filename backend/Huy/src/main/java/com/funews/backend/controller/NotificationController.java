package com.funews.backend.controller;

import com.funews.backend.dto.NotificationResponse;
import com.funews.backend.exception.ApiException;
import com.funews.backend.security.AccountPrincipal;
import com.funews.backend.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hop thu cua nguoi dang dang nhap - AC-ED-002.
 *
 * Khong co endpoint nao gui thong bao: thong bao chi sinh ra tu luong duyet bai
 * trong NewsService, khong ai goi API de tu gui cho nguoi khac.
 */
@Tag(name = "5. Notifications", description = "Thong bao cua luong bien tap. Phu trach: Huy")
@RestController
@RequestMapping("/api/notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {
    private final NewsService service;

    public NotificationController(NewsService service) {
        this.service = service;
    }

    public record UnreadCount(long count) {}

    @Operation(summary = "Hop thu cua chinh minh, moi nhat truoc")
    @GetMapping
    public List<NotificationResponse> inbox(Authentication authentication) {
        return service.inbox(actor(authentication));
    }

    @Operation(summary = "So thong bao chua doc, dung cho cai chuong tren thanh tieu de")
    @GetMapping("/unread-count")
    public UnreadCount unreadCount(Authentication authentication) {
        return new UnreadCount(service.unreadCount(actor(authentication)));
    }

    @Operation(summary = "Danh dau mot thong bao la da doc")
    @PostMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable Long id, Authentication authentication) {
        return service.markNotificationRead(id, actor(authentication));
    }

    private AccountPrincipal actor(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal) {
            return principal;
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, "Vui long dang nhap.");
    }
}
