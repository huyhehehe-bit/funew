package com.funews.backend.controller;

import com.funews.backend.dto.ApiDtos.AccountResponse;
import com.funews.backend.exception.ApiException;
import com.funews.backend.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "1. Auth", description = "Dang nhap va phien lam viec. Phu trach: QA")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final SessionAuthenticationStrategy sessions;
    private final SecurityContextRepository contexts;
    private final AccountService management;

    public AuthController(AuthenticationManager authenticationManager, SessionAuthenticationStrategy sessions,
            SecurityContextRepository contexts, AccountService management) {
        this.authenticationManager = authenticationManager;
        this.sessions = sessions;
        this.contexts = contexts;
        this.management = management;
    }

    public record LoginRequest(@NotBlank @Size(max = 255) String username,
                               @NotBlank @Size(max = 72) String password) {}

    @Operation(summary = "Dang nhap, tao phien lam viec")
    @PostMapping("/login")
    public AccountResponse login(@Valid @RequestBody LoginRequest body,
            HttpServletRequest request, HttpServletResponse response) {
        if (body.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password must contain at most 72 UTF-8 bytes.");
        }
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(body.username(), body.password()));
        sessions.onAuthentication(authentication, request, response);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
        return management.currentAccount(authentication.getName());
    }

    @Operation(summary = "Thong tin tai khoan dang dang nhap")
    @GetMapping("/me")
    public AccountResponse me(Authentication authentication) {
        return management.currentAccount(authentication.getName());
    }
}
