package com.funews.backend.security;

import com.funews.backend.repository.AccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Keep sessions consistent when an account's email or role changes. */
public class RefreshAccountFilter extends OncePerRequestFilter {
    private final AccountRepository accounts;

    public RefreshAccountFilter(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var context = SecurityContextHolder.getContext();
        var authentication = context.getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal existing) {
            var account = accounts.findById(existing.id());
            if (account.isEmpty()) {
                SecurityContextHolder.clearContext();
                if (request.getSession(false) != null) {
                    request.getSession(false).invalidate();
                }
            } else {
                var principal = AccountPrincipal.from(account.get());
                context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                        principal, null, principal.getAuthorities()));
            }
        }
        chain.doFilter(request, response);
    }
}
