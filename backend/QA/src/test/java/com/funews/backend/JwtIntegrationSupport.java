package com.funews.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.funews.backend.config.SecurityConfig;
import com.funews.backend.controller.AccountController;
import com.funews.backend.controller.AuthController;
import com.funews.backend.dto.AccountResponse;
import com.funews.backend.entity.Account;
import com.funews.backend.entity.Role;
import com.funews.backend.exception.ApiExceptionHandler;
import com.funews.backend.repository.AccountRepository;
import com.funews.backend.security.JwtTokenService;
import com.funews.backend.service.AccountService;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** QA-only web context: real controllers/security, mocked persistence and account service. */
abstract class JwtIntegrationSupport {
    protected static final Instant NOW = Instant.parse("2026-10-25T04:00:00Z");
    protected static final String SECRET = Base64.getEncoder().encodeToString(
            "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));
    protected AnnotationConfigWebApplicationContext context;
    protected MockMvc mvc;
    protected ObjectMapper mapper;
    protected AccountRepository accounts;
    protected AccountService service;
    protected JwtTokenService tokens;
    protected Account account;
    protected PasswordEncoder encoder;

    @BeforeEach
    void startQaWebContext() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(Fixture.class);
        context.refresh();
        mapper = context.getBean(ObjectMapper.class);
        accounts = context.getBean(AccountRepository.class);
        service = context.getBean(AccountService.class);
        tokens = context.getBean(JwtTokenService.class);
        encoder = context.getBean(PasswordEncoder.class);
        Role role = new Role();
        role.setId(2L);
        role.setCode("EDITOR");
        role.setName("Editor");
        account = new Account();
        account.setId(42L);
        account.setUsername("editor");
        account.setEmail("editor@fu.edu.vn");
        account.setFullName("QA Editor");
        account.setRole(role);
        account.setPasswordHash(encoder.encode("123"));
        when(accounts.findById(42L)).thenAnswer(invocation -> Optional.of(account));
        when(accounts.findByEmailIgnoreCase(anyString())).thenAnswer(invocation ->
                account.getEmail().equalsIgnoreCase(invocation.getArgument(0)) ? Optional.of(account) : Optional.empty());
        when(accounts.findByUsernameIgnoreCaseAndDeletedFalse(anyString())).thenAnswer(invocation ->
                account.getUsername().equalsIgnoreCase(invocation.getArgument(0)) && !account.isDeleted()
                        ? Optional.of(account) : Optional.empty());
        when(service.currentAccount(anyString())).thenAnswer(invocation -> accountResponse());
        when(service.listAccounts()).thenAnswer(invocation -> List.of(accountResponse()));
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterEach
    void stopQaWebContext() { context.close(); }

    protected AccountResponse accountResponse() {
        return new AccountResponse(account.getId(), account.getFullName(), account.getEmail(),
                account.getRole().getId(), account.getRole().getCode(), account.isActive(),
                account.getUsername(), account.getDateOfBirth(), account.getCreatedAt(), account.getStatus());
    }

    protected String login() throws Exception { return login(account.getEmail(), "123"); }

    protected String login(String username, String password) throws Exception {
        return body(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("accessToken").asText();
    }

    protected JsonNode body(String json) throws Exception { return mapper.readTree(json); }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, AuthController.class, AccountController.class, ApiExceptionHandler.class,
            NewsFixture.class})
    static class Fixture {
        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
        @Bean AccountRepository accounts() { return mock(AccountRepository.class); }
        @Bean AccountService accountService() { return mock(AccountService.class); }
        @Bean JwtTokenService tokens(ObjectMapper mapper) {
            return new JwtTokenService(mapper, SECRET, 900, "funews", "funews-api", Clock.fixed(NOW, ZoneOffset.UTC));
        }
        @Bean CorsConfigurationSource corsConfigurationSource() {
            CorsConfiguration policy = new CorsConfiguration();
            policy.setAllowedOrigins(List.of("http://localhost:5173"));
            policy.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
            policy.setAllowedHeaders(List.of("Content-Type"));
            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/api/**", policy);
            return source;
        }
    }

    /** Only exposes successful handlers so tests can isolate the QA security rules. */
    @RestController
    static class NewsFixture {
        @RequestMapping(path = {"/api/news", "/api/news/{id}"},
                method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
        ResponseEntity<Void> news() {
            return ResponseEntity.noContent().build();
        }
    }
}
