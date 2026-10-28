package com.funews.backend.security;

import com.funews.backend.entity.Account;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record AccountPrincipal(Long id, String email, String passwordHash, String roleCode,
                               boolean active, boolean deleted) implements UserDetails {
    public static AccountPrincipal from(Account account) {
        return new AccountPrincipal(account.getId(), account.getEmail(), account.getPasswordHash(),
                account.getRole().getCode(), account.isActive() && account.getRole().isActive(), account.isDeleted());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String code = roleCode.toUpperCase(Locale.ROOT);
        if ("EDITOR".equals(code) || "CONTRIBUTOR".equals(code)) {
            return List.of(new SimpleGrantedAuthority("ROLE_" + code), new SimpleGrantedAuthority("ROLE_STAFF"));
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + code));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return active && !deleted;
    }
}
