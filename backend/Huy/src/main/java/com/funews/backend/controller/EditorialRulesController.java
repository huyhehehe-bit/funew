package com.funews.backend.controller;

import com.funews.backend.dto.EditorialRulesRequest;
import com.funews.backend.dto.EditorialRulesResponse;
import com.funews.backend.exception.ApiException;
import com.funews.backend.security.AccountPrincipal;
import com.funews.backend.service.EditorialRulesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quy tac bien tap - SRS muc 3.2.
 *
 * Ai dang nhap cung doc duoc (tac gia can biet quy tac truoc khi nop bai),
 * nhung chi ADMIN sua duoc - gioi han o SecurityConfig.
 */
@Tag(name = "6. Editorial rules", description = "Quy tac bien tap do Administrator cau hinh. Phu trach: Huy")
@RestController
@RequestMapping("/api/editorial-rules")
@SecurityRequirement(name = "bearerAuth")
public class EditorialRulesController {
    private final EditorialRulesService service;

    public EditorialRulesController(EditorialRulesService service) {
        this.service = service;
    }

    @Operation(summary = "Quy tac dang ap dung")
    @GetMapping
    public EditorialRulesResponse rules() {
        return service.get();
    }

    @Operation(summary = "Sua quy tac - chi Administrator")
    @PutMapping
    public EditorialRulesResponse update(@Valid @RequestBody EditorialRulesRequest request,
                                         Authentication authentication) {
        return service.update(request, actor(authentication));
    }

    private AccountPrincipal actor(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal) {
            return principal;
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, "Vui long dang nhap.");
    }
}
