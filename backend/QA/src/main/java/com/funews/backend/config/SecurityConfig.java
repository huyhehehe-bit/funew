package com.funews.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.funews.backend.repository.AccountRepository;
import com.funews.backend.security.AccountPrincipal;
import com.funews.backend.security.JwtAuthenticationFilter;
import com.funews.backend.security.JwtTokenService;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import java.util.ArrayList;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableMethodSecurity
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(AccountRepository accounts) {
        return username -> {
            String login = username.trim();
            var account = "Admin".equalsIgnoreCase(login)
                    ? accounts.findByEmailIgnoreCase("admin@fu.edu.vn")
                    : accounts.findByEmailIgnoreCase(login)
                            .or(() -> accounts.findByUsernameIgnoreCaseAndDeletedFalse(login));
            return account.map(AccountPrincipal::from)
                    .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
        };
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            @Qualifier("corsConfigurationSource") CorsConfigurationSource cors,
            AccountRepository accounts, JwtTokenService tokens, ObjectMapper mapper) throws Exception {
        http.cors(config -> config.configurationSource(bearerCors(cors)))
                // Credentials are accepted only in explicit Authorization headers, never cookies.
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext(config -> config.securityContextRepository(new NullSecurityContextRepository()))
                .requestCache(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/error").permitAll()
                        // Swagger UI va file OpenAPI: mo cong khai de xem tai lieu API.
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs", "/v3/api-docs/**", "/swagger-resources/**").permitAll()
                        // Luong duyet bai - NFR-ED-003: duyet, dang va go bai chi EDITOR va ADMIN.
                        // PHAI khai TRUOC cac rule permitAll ben duoi: "/api/news/*" khop ca
                        // "/api/news/review-queue", rule nao khai truoc thi thang.
                        .requestMatchers(HttpMethod.GET, "/api/news/review-queue").hasAnyRole("ADMIN", "EDITOR")
                        .requestMatchers(HttpMethod.POST, "/api/news/*/request-changes", "/api/news/*/approve",
                                "/api/news/*/publish", "/api/news/*/archive").hasAnyRole("ADMIN", "EDITOR")
                        // Nop bai la viec cua tac gia, nen CONTRIBUTOR cung vao duoc.
                        .requestMatchers(HttpMethod.POST, "/api/news/*/submit")
                                .hasAnyRole("ADMIN", "EDITOR", "CONTRIBUTOR")
                        // Nhat ky va hop thu: phai dang nhap, con thay gi thi tang service loc.
                        .requestMatchers(HttpMethod.GET, "/api/news/*/audit").authenticated()
                        .requestMatchers("/api/notifications", "/api/notifications/**").authenticated()
                        // Doc cong khai: khach chua dang nhap van xem duoc tin tuc, danh muc
                        // va ten tac gia. Chi mo phuong thuc GET; moi thao tac ghi van phai dang nhap.
                        .requestMatchers(HttpMethod.GET, "/api/news", "/api/news/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories", "/api/categories/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/accounts/lookup").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/news", "/api/news/*")
                                .hasAnyRole("ADMIN", "EDITOR", "CONTRIBUTOR")
                        .requestMatchers(HttpMethod.PUT, "/api/news", "/api/news/*")
                                .hasAnyRole("ADMIN", "EDITOR", "CONTRIBUTOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/news", "/api/news/*")
                                .hasAnyRole("ADMIN", "EDITOR", "CONTRIBUTOR")
                        .requestMatchers("/api/accounts/lookup").authenticated()
                        .requestMatchers("/api/accounts", "/api/accounts/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/categories", "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(config -> config
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(401);
                            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            mapper.writeValue(response.getOutputStream(), Map.of("message", "Vui long dang nhap."));
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(403);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            mapper.writeValue(response.getOutputStream(), Map.of("message",
                                    "Ban khong co quyen thuc hien thao tac nay."));
                        }))
                .addFilterBefore(new JwtAuthenticationFilter(tokens, accounts, mapper), AnonymousAuthenticationFilter.class);
        return http.build();
    }

    // Extend the shared CORS policy here, within QA ownership, for Bearer requests.
    private CorsConfigurationSource bearerCors(CorsConfigurationSource source) {
        return request -> {
            CorsConfiguration original = source.getCorsConfiguration(request);
            if (original == null) {
                return null;
            }
            CorsConfiguration configuration = new CorsConfiguration(original);
            var headers = original.getAllowedHeaders() == null
                    ? new ArrayList<String>() : new ArrayList<>(original.getAllowedHeaders());
            headers.add(HttpHeaders.AUTHORIZATION);
            configuration.setAllowedHeaders(headers);
            configuration.setAllowCredentials(false);
            return configuration;
        };
    }
}
