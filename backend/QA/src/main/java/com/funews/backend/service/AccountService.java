package com.funews.backend.service;

import com.funews.backend.dto.AccountLookup;
import com.funews.backend.dto.AccountRequest;
import com.funews.backend.dto.AccountResponse;
import com.funews.backend.dto.ProfileRequest;
import com.funews.backend.dto.RoleResponse;
import com.funews.backend.entity.Account;
import com.funews.backend.entity.Role;
import com.funews.backend.exception.ApiException;
import com.funews.backend.repository.AccountRepository;
import com.funews.backend.repository.RoleRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
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
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roles;

    public AccountService(AccountRepository accounts, PasswordEncoder passwordEncoder, RoleRepository roles) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.roles = roles;
    }

    public List<AccountResponse> listAccounts() {
        return accounts.findByDeletedFalseOrderByIdAsc().stream().map(this::accountResponse).toList();
    }

    public List<AccountLookup> lookupAccounts() {
        return accounts.findByDeletedFalseOrderByIdAsc().stream()
                .map(account -> new AccountLookup(account.getId(), account.getFullName())).toList();
    }

    public List<RoleResponse> listRoles() {
        return roles.findByActiveTrueOrderByIdAsc().stream()
                .map(role -> new RoleResponse(role.getId(), role.getCode(), role.getName(), role.getDescription(),
                        role.isActive(), role.getCreatedAt())).toList();
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
        String username = normalizeUsername(request.username() == null ? email : request.username());
        ensureUsernameAvailable(username, null);
        boolean active = resolveActive(request, true);
        Role role = requireRole(request.roleId());
        if (request.password() == null || request.password().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password is required when creating an account.");
        }
        validatePassword(request.password());
        Account account = new Account();
        account.setFullName(request.fullName().trim());
        account.setEmail(email);
        account.setUsername(username);
        account.setDateOfBirth(request.dateOfBirth());
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        account.setRole(role);
        account.setActive(active);
        return accountResponse(accounts.save(account));
    }

    @Transactional
    public AccountResponse updateAccount(Long id, AccountRequest request) {
        // Serialize account changes before reading the last-administrator invariant.
        roles.lockAdministratorRole();
        Account account = requireAccount(id);
        String email = normalizeEmail(request.email());
        ensureEmailAvailable(email, id);
        Role role = requireRole(request.roleId());
        boolean active = resolveActive(request, account.isActive());
        protectLastAdministrator(account, !"ADMIN".equals(role.getCode()) || !active);
        if (request.username() != null) {
            String username = normalizeUsername(request.username());
            ensureUsernameAvailable(username, id);
            account.setUsername(username);
        }
        if (request.dateOfBirth() != null) {
            account.setDateOfBirth(request.dateOfBirth());
        }
        account.setFullName(request.fullName().trim());
        account.setEmail(email);
        account.setRole(role);
        account.setActive(active);
        if (request.password() != null && !request.password().isBlank()) {
            validatePassword(request.password());
            account.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return accountResponse(accounts.save(account));
    }

    @Transactional
    public void deleteAccount(Long id, String currentEmail) {
        roles.lockAdministratorRole();
        Account account = requireAccount(id);
        if (account.getEmail().equalsIgnoreCase(currentEmail)) {
            throw new ApiException(HttpStatus.CONFLICT, "You cannot delete the account currently signed in.");
        }
        protectLastAdministrator(account, true);
        if (accounts.countArticlesByAuthorId(id) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "This account has news articles and cannot be deleted.");
        }
        account.setDeleted(true);
        account.setActive(false);
        account.setDeletedAt(LocalDateTime.now());
        account.setDeletedBy(requireCurrentAccount(currentEmail));
        accounts.save(account);
    }

    @Transactional
    public AccountResponse updateProfile(String currentEmail, ProfileRequest request) {
        roles.lockAdministratorRole();
        Account account = requireCurrentAccount(currentEmail);
        String email = normalizeEmail(request.email());
        ensureEmailAvailable(email, account.getId());
        if (request.username() != null) {
            String username = normalizeUsername(request.username());
            ensureUsernameAvailable(username, account.getId());
            account.setUsername(username);
        }
        if (request.dateOfBirth() != null) {
            account.setDateOfBirth(request.dateOfBirth());
        }
        account.setFullName(request.fullName().trim());
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
                .filter(account -> !account.isDeleted())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found."));
    }

    private Account requireCurrentAccount(String email) {
        return accounts.findByEmailIgnoreCase(email)
                .filter(Account::isActive)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Please sign in again."));
    }

    private AccountResponse accountResponse(Account account) {
        return new AccountResponse(account.getId(), account.getFullName(), account.getEmail(),
                account.getRole().getId(), account.getRole().getCode(), account.isActive(), account.getUsername(),
                account.getDateOfBirth(), account.getCreatedAt(), account.getStatus());
    }

    private Role requireRole(Long id) {
        return roles.findById(id).filter(Role::isActive)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Role is invalid or inactive."));
    }

    private void ensureUsernameAvailable(String username, Long accountId) {
        accounts.findByUsernameIgnoreCaseAndDeletedFalse(username).ifPresent(existing -> {
            if (!existing.getId().equals(accountId)) {
                throw new ApiException(HttpStatus.CONFLICT, "This username is already in use.");
            }
        });
    }

    private String normalizeUsername(String username) {
        String normalized = username.trim();
        if (normalized.isBlank() || normalized.length() > 254) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Username must contain 1 to 254 characters.");
        }
        return normalized;
    }

    private boolean resolveActive(AccountRequest request, boolean current) {
        if (request.status() != null && request.status() != 0 && request.status() != 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Status must be 0 or 1.");
        }
        if (request.status() != null && request.isActive() != null
                && request.isActive() != (request.status() == 1)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Status and isActive must agree.");
        }
        return request.status() != null ? request.status() == 1
                : request.isActive() != null ? request.isActive() : current;
    }

    private void protectLastAdministrator(Account account, boolean removesAccess) {
        if (removesAccess && account.isActive() && "ADMIN".equals(account.getRole().getCode())
                && accounts.countByRole_CodeAndActiveTrueAndDeletedFalse("ADMIN") <= 1) {
            throw new ApiException(HttpStatus.CONFLICT, "The last active administrator cannot be removed or disabled.");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
