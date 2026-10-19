package com.funews.backend.controller;

import com.funews.backend.dto.AccountLookup;
import com.funews.backend.dto.AccountRequest;
import com.funews.backend.dto.AccountResponse;
import com.funews.backend.dto.ProfileRequest;
import com.funews.backend.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "2. Accounts", description = "Quan ly tai khoan va ho so ca nhan. Phu trach: QA")
@RestController
@RequestMapping("/api")
public class AccountController {
    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @Operation(summary = "Danh sach tai khoan - chi Admin")
    @GetMapping("/accounts")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AccountResponse> accounts() {
        return service.listAccounts();
    }

    @Operation(summary = "ID va ten tac gia - Admin va Staff deu goi duoc")
    @GetMapping("/accounts/lookup")
    public List<AccountLookup> accountLookup() {
        return service.lookupAccounts();
    }

    @Operation(summary = "Xem mot tai khoan - chi Admin")
    @GetMapping("/accounts/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AccountResponse account(@PathVariable Long id) {
        return service.getAccount(id);
    }

    @Operation(summary = "Tao tai khoan moi - chi Admin")
    @PostMapping("/accounts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody AccountRequest request) {
        AccountResponse created = service.createAccount(request);
        return ResponseEntity.created(URI.create("/api/accounts/" + created.id())).body(created);
    }

    @Operation(summary = "Sua tai khoan - de trong Password de giu mat khau cu")
    @PutMapping("/accounts/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AccountResponse updateAccount(@PathVariable Long id, @Valid @RequestBody AccountRequest request) {
        return service.updateAccount(id, request);
    }

    @Operation(summary = "Xoa tai khoan - khong xoa duoc chinh minh hay Admin cuoi cung")
    @DeleteMapping("/accounts/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id, Authentication authentication) {
        service.deleteAccount(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Xem ho so cua chinh minh")
    @GetMapping("/profile")
    public AccountResponse profile(Authentication authentication) {
        return service.currentAccount(authentication.getName());
    }

    @Operation(summary = "Cap nhat ten va email cua chinh minh")
    @PutMapping("/profile")
    public AccountResponse updateProfile(Authentication authentication, @Valid @RequestBody ProfileRequest request) {
        return service.updateProfile(authentication.getName(), request);
    }
}
