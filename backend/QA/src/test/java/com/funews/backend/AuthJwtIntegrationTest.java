package com.funews.backend;

import com.funews.backend.entity.Account;
import com.funews.backend.security.AccountPrincipal;
import com.funews.backend.security.JwtTokenService;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthJwtIntegrationTest extends JwtIntegrationSupport {
    @Test
    void storedUsernameAndEmailAcceptTrimmedCaseInsensitiveLogin() throws Exception {
        account.setUsername("custom.editor");
        for (String identifier : new String[]{"  CUSTOM.Editor  ", "  EDITOR@FU.EDU.VN  "}) {
            assertThat(tokens.verify(login(identifier, "123")).accountId()).isEqualTo(account.getId());
        }
    }

    @Test
    void emailTakesPriorityOverAnotherAccountsMatchingUsername() throws Exception {
        // A username can resemble another account's email; keep email login deterministic.
        Account other = new Account();
        other.setId(43L);
        other.setUsername(account.getEmail());
        other.setEmail("another@fu.edu.vn");
        other.setRole(account.getRole());
        other.setPasswordHash(account.getPasswordHash());
        when(accounts.findByUsernameIgnoreCaseAndDeletedFalse(account.getEmail())).thenReturn(Optional.of(other));
        assertThat(tokens.verify(login(account.getEmail(), "123")).accountId()).isEqualTo(account.getId());
        verify(accounts, never()).findByUsernameIgnoreCaseAndDeletedFalse(account.getEmail());
    }

    @Test
    void adminAliasStillTargetsTheAdminEmailInsteadOfStoredUsername() throws Exception {
        account.setEmail("admin@fu.edu.vn");
        account.setUsername("different.admin.username");
        assertThat(tokens.verify(login("  aDmIn  ", "123")).accountId()).isEqualTo(account.getId());
        verify(accounts, never()).findByUsernameIgnoreCaseAndDeletedFalse("aDmIn");
    }

    @Test
    void adminAliasCannotFallBackToAnotherAccountsStoredUsername() throws Exception {
        account.setUsername("Admin");
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", "Admin", "password", "123"))))
                .andExpect(status().isUnauthorized());
        verify(accounts, never()).findByUsernameIgnoreCaseAndDeletedFalse("Admin");
    }

    @Test
    void readerCanReadNewsButCannotCreateUpdateOrDeleteIt() throws Exception {
        account.getRole().setCode("READER");
        String token = login();
        for (String path : new String[]{"/api/news", "/api/news/1"}) {
            mvc.perform(get(path)).andExpect(status().isNoContent());
            for (HttpMethod method : new HttpMethod[]{HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE}) {
                mvc.perform(request(method, path).header("Authorization", "Bearer " + token))
                        .andExpect(status().isForbidden());
            }
        }
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void adminEditorAndContributorKeepNewsWriteAccess() throws Exception {
        String token = login();
        for (String role : new String[]{"ADMIN", "EDITOR", "CONTRIBUTOR"}) {
            account.getRole().setCode(role);
            for (String path : new String[]{"/api/news", "/api/news/1"}) {
                for (HttpMethod method : new HttpMethod[]{HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE}) {
                    mvc.perform(request(method, path).header("Authorization", "Bearer " + token))
                            .andExpect(status().isNoContent());
                }
            }
        }
    }

    @Test
    void loginReturnsJwtAndUserWithoutCreatingSessionOrExposingCredentials() throws Exception {
        var result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", account.getEmail(), "password", "123"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.id").value(42))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist()).andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
        String token = body(result.getResponse().getContentAsString()).path("accessToken").asText();
        assertThat(tokens.verify(token).accountId()).isEqualTo(42L);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(account.getEmail()));
    }

    @Test
    void missingMalformedTamperedAndExpiredTokensAreUnauthorized() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"));
        String valid = login();
        String expired = new JwtTokenService(mapper, SECRET, 900, "funews", "funews-api",
                Clock.fixed(NOW.minusSeconds(901), ZoneOffset.UTC)).issue(AccountPrincipal.from(account)).accessToken();
        for (String value : new String[]{"Basic abc", "Bearer invalid", "Bearer " + valid + "x", "Bearer " + expired}) {
            mvc.perform(get("/api/auth/me").header("Authorization", value))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").isNotEmpty());
        }
    }

    @Test
    void oldSessionCannotAuthenticateAndDuplicateAuthorizationIsRejected() throws Exception {
        MockHttpSession old = new MockHttpSession();
        old.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(UsernamePasswordAuthenticationToken.authenticated(
                        AccountPrincipal.from(account), null, AccountPrincipal.from(account).getAuthorities())));
        mvc.perform(get("/api/auth/me").session(old)).andExpect(status().isUnauthorized());
        String bearer = "Bearer " + login();
        mvc.perform(get("/api/auth/me").header("Authorization", bearer, bearer)).andExpect(status().isUnauthorized());
    }

    @Test
    void roleChangesTakeEffectOnNextRequestWithSameToken() throws Exception {
        account.getRole().setCode("ADMIN");
        String token = login();
        mvc.perform(get("/api/accounts").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        account.getRole().setCode("EDITOR");
        mvc.perform(get("/api/accounts").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.roleCode").value("EDITOR"));
    }

    @Test
    void disablingDeletingOrDeactivatingRoleRejectsExistingToken() throws Exception {
        String token = login();
        account.setActive(false);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        account.setActive(true);
        account.setDeleted(true);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        account.setDeleted(false);
        account.getRole().setActive(false);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesCurrentTokenAndLeavesOtherLoginUsable() throws Exception {
        String first = login();
        String second = login();
        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + first))
                .andExpect(status().isNoContent()).andExpect(header().doesNotExist("Set-Cookie"));
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + first)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + second)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout")).andExpect(status().isUnauthorized());
    }

    @Test
    void emailChangeKeepsTokenValidButPasswordChangeInvalidatesIt() throws Exception {
        String token = login();
        account.setEmail("changed@fu.edu.vn");
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("changed@fu.edu.vn"));
        account.setPasswordHash(encoder.encode("changed-password"));
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test
    void accountMissingFromRepositoryCannotUseToken() throws Exception {
        String token = login();
        when(accounts.findById(42L)).thenReturn(java.util.Optional.empty());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test
    void staleAuthorizationCannotBlockNewLogin() throws Exception {
        mvc.perform(post("/api/auth/login").header("Authorization", "Bearer expired-or-broken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", account.getEmail(), "password", "123"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void corsAllowsBearerPreflightOnlyFromAllowedOrigin() throws Exception {
        mvc.perform(options("/api/auth/me").header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
        mvc.perform(options("/api/auth/me").header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isForbidden());
    }
}
