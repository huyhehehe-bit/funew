package com.funews.backend.security;

import com.funews.backend.entity.Account;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record AccountPrincipal(Long id, String email, String passwordHash, int role) implements UserDetails {
    public static AccountPrincipal from(Account account) {
        return new AccountPrincipal(account.getId(), account.getEmail(), account.getPasswordHash(), account.getRole());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role == 1 ? "ROLE_ADMIN" : "ROLE_STAFF"));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
