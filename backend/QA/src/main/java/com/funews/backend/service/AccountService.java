package com.funews.backend.service;

import com.funews.backend.dto.ApiDtos.AccountLookup;
import com.funews.backend.dto.ApiDtos.AccountRequest;
import com.funews.backend.dto.ApiDtos.AccountResponse;
import com.funews.backend.dto.ApiDtos.ProfileRequest;
import com.funews.backend.entity.Account;
import com.funews.backend.exception.ApiException;
import com.funews.backend.repository.AccountRepository;
import com.funews.backend.repository.NewsArticleRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AccountService {
    private final AccountRepository accounts;
    private final NewsArticleRepository news;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AccountRepository accounts, NewsArticleRepository news, PasswordEncoder passwordEncoder) {
        this.accounts = accounts;
        this.news = news;
        this.passwordEncoder = passwordEncoder;
    }

    public List<AccountResponse> listAccounts() {
        return accounts.findAllByOrderByIdAsc().stream().map(this::accountResponse).toList();
    }

    public List<AccountLookup> lookupAccounts() {
        return accounts.findAllByOrderByIdAsc().stream()
                .map(account -> new AccountLookup(account.getId(), account.getName())).toList();
    }

    public AccountResponse getAccount(Long id) {
        return accountResponse(requireAccount(id));
    }

    public AccountResponse currentAccount(String email) {
        return accountResponse(requireCurrentAccount(email));
    }

    @Transactional
    public AccountResponse createAccount(AccountRequest request) {
        String email = normalizeEmail(request.email());
        ensureEmailAvailable(email, null);
        if (request.password() == null || request.password().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password is required when creating an account.");
        }
        validatePassword(request.password());
        Account account = new Account();
        account.setName(request.name().trim());
        account.setEmail(email);
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        account.setRole(request.role());
        return accountResponse(accounts.save(account));
    }

    @Transactional
    public AccountResponse updateAccount(Long id, AccountRequest request) {
        Account account = requireAccount(id);
        String email = normalizeEmail(request.email());
        ensureEmailAvailable(email, id);
        if (account.getRole() == 1 && request.role() != 1 && accounts.countByRole(1) <= 1) {
            throw new ApiException(HttpStatus.CONFLICT, "The last administrator cannot be demoted.");
        }
        account.setName(request.name().trim());
        account.setEmail(email);
        account.setRole(request.role());
        if (request.password() != null && !request.password().isBlank()) {
            validatePassword(request.password());
            account.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return accountResponse(accounts.save(account));
    }

    @Transactional
    public void deleteAccount(Long id, String currentEmail) {
        Account account = requireAccount(id);
        if (account.getEmail().equalsIgnoreCase(currentEmail)) {
            throw new ApiException(HttpStatus.CONFLICT, "You cannot delete the account currently signed in.");
        }
        if (account.getRole() == 1 && accounts.countByRole(1) <= 1) {
            throw new ApiException(HttpStatus.CONFLICT, "The last administrator cannot be deleted.");
        }
        if (news.existsByCreatedById(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "This account has news articles and cannot be deleted.");
        }
        accounts.delete(account);
    }

    @Transactional
    public AccountResponse updateProfile(String currentEmail, ProfileRequest request) {
        Account account = requireCurrentAccount(currentEmail);
        String email = normalizeEmail(request.email());
        ensureEmailAvailable(email, account.getId());
        account.setName(request.name().trim());
        account.setEmail(email);
        return accountResponse(accounts.save(account));
    }

    private void validatePassword(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password must contain at most 72 UTF-8 bytes.");
        }
    }

    private void ensureEmailAvailable(String email, Long accountId) {
        accounts.findByEmailIgnoreCase(email).ifPresent(existing -> {
            if (!existing.getId().equals(accountId)) {
                throw new ApiException(HttpStatus.CONFLICT, "This email address is already in use.");
            }
        });
    }

    private Account requireAccount(Long id) {
        return accounts.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found."));
    }

    private Account requireCurrentAccount(String email) {
        return accounts.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Please sign in again."));
    }

    private AccountResponse accountResponse(Account account) {
        return new AccountResponse(account.getId(), account.getName(), account.getEmail(), account.getRole());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
