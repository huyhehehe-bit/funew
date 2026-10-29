package com.funews.backend.controller;

import com.funews.backend.dto.AccountResponse;
import com.funews.backend.dto.AuthResponse;
import com.funews.backend.dto.RegisterRequest;
import com.funews.backend.exception.ApiException;
import com.funews.backend.security.AccountPrincipal;
import com.funews.backend.security.JwtAuthenticationFilter;
import com.funews.backend.security.JwtTokenService;
import com.funews.backend.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "1. Auth", description = "Dang nhap va xac thuc JWT Bearer. Phu trach: QA")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService tokens;
    private final AccountService management;

    public AuthController(AuthenticationManager authenticationManager, JwtTokenService tokens, AccountService management) {
        this.authenticationManager = authenticationManager;
        this.tokens = tokens;
        this.management = management;
    }

    public record LoginRequest(@NotBlank @Size(max = 255) String username,
                               @NotBlank @Size(max = 72) String password) {}

    @Operation(summary = "Dang nhap, nhan JWT access token")
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest body, HttpServletResponse response) {
        if (body.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password must contain at most 72 UTF-8 bytes.");
        }
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(body.username(), body.password()));
        var token = tokens.issue((AccountPrincipal) authentication.getPrincipal());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        return new AuthResponse(token.accessToken(), "Bearer", token.expiresIn(),
                management.currentAccount(authentication.getName()));
    }

    @Operation(summary = "Dang ky tai khoan doc gia - mo cong khai")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest body,
                                                 HttpServletResponse response) {
        AccountResponse created = management.register(body);
        // Dang nhap luon bang chinh duong dang nhap that: vua tien cho nguoi dung,
        // vua chung minh tai khoan vua tao thuc su dang nhap duoc.
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(created.email(), body.password()));
        var token = tokens.issue((AccountPrincipal) authentication.getPrincipal());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(token.accessToken(), "Bearer", token.expiresIn(), created));
    }

    @Operation(summary = "Thong tin tai khoan dang dang nhap")
    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public AccountResponse me(Authentication authentication) {
        return management.currentAccount(authentication.getName());
    }

    @Operation(summary = "Dang xuat, thu hoi JWT hien tai")
    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        var token = (JwtTokenService.VerifiedToken) request.getAttribute(JwtAuthenticationFilter.VERIFIED_TOKEN_ATTRIBUTE);
        if (token == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Bearer access token is required.");
        }
        tokens.revoke(token);
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().header(HttpHeaders.CACHE_CONTROL, "no-store").build();
    }
}
