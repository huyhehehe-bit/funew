package com.funews.backend.dto;

/** Login result. AccountResponse deliberately excludes credentials. */
public record AuthResponse(String accessToken, String tokenType, long expiresIn, AccountResponse user) {}
