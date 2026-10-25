package com.funews.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.funews.backend.repository.AccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Authenticate only explicit Bearer headers; cookies and sessions cannot authenticate a request. */
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    public static final String VERIFIED_TOKEN_ATTRIBUTE = JwtAuthenticationFilter.class.getName() + ".token";
    private final JwtTokenService tokens;
    private final AccountRepository accounts;
    private final ObjectMapper mapper;

    public JwtAuthenticationFilter(JwtTokenService tokens, AccountRepository accounts, ObjectMapper mapper) {
        this.tokens = tokens;
        this.accounts = accounts;
        this.mapper = mapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equals(request.getMethod())
                || ("POST".equals(request.getMethod())
                    && (request.getContextPath() + "/api/auth/login").equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var headers = Collections.list(request.getHeaders(HttpHeaders.AUTHORIZATION));
        if (headers.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }
        try {
            String header = headers.get(0);
            if (headers.size() != 1 || header.length() < 8 || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
                throw new BadCredentialsException("Bearer access token is required.");
            }
            var token = tokens.verify(header.substring(7));
            AccountPrincipal principal = accounts.findById(token.accountId()).map(AccountPrincipal::from)
                    .filter(AccountPrincipal::isEnabled)
                    .orElseThrow(() -> new BadCredentialsException("Account is inactive or unavailable."));
            if (!tokens.matchesCredentials(token, principal)) {
                throw new BadCredentialsException("Account credentials have changed.");
            }
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                    principal, null, principal.getAuthorities()));
            SecurityContextHolder.setContext(context);
            request.setAttribute(VERIFIED_TOKEN_ATTRIBUTE, token);
        } catch (AuthenticationException exception) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\"");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
            mapper.writeValue(response.getOutputStream(), Map.of("message", "Access token is invalid or expired."));
            return;
        }
        chain.doFilter(request, response);
    }
}
