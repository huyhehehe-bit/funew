package com.funews.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.funews.backend.repository.AccountRepository;
import com.funews.backend.security.AccountPrincipal;
import com.funews.backend.security.RefreshAccountFilter;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(AccountRepository accounts) {
        return username -> accounts.findByEmailIgnoreCase(
                        "Admin".equalsIgnoreCase(username.trim()) ? "admin@fu.edu.vn" : username.trim())
                .map(AccountPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        // Doi session id ngay sau khi dang nhap de chan tan cong session fixation.
        return new CompositeSessionAuthenticationStrategy(List.of(new ChangeSessionIdAuthenticationStrategy()));
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            @Qualifier("corsConfigurationSource") CorsConfigurationSource cors,
            SecurityContextRepository contexts,
            AccountRepository accounts, ObjectMapper mapper) throws Exception {
        http.cors(config -> config.configurationSource(cors))
                // Khong dung CSRF token. Cookie phien da dat SameSite=lax nen trinh duyet
                // khong gui kem cookie khi bi trang khac POST sang - do la lop chan chinh.
                .csrf(config -> config.disable())
                .securityContext(config -> config.securityContextRepository(contexts))
                .requestCache(config -> config.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/error").permitAll()
                        // Swagger UI va file OpenAPI: mo cong khai de xem tai lieu API.
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs", "/v3/api-docs/**", "/swagger-resources/**").permitAll()
                        // Doc cong khai: khach chua dang nhap van xem duoc tin tuc, danh muc
                        // va ten tac gia. Chi mo phuong thuc GET; moi thao tac ghi van phai dang nhap.
                        .requestMatchers(HttpMethod.GET, "/api/news", "/api/news/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories", "/api/categories/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/accounts/lookup").permitAll()
                        .requestMatchers("/api/accounts/lookup").authenticated()
                        .requestMatchers("/api/accounts", "/api/accounts/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/categories", "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(config -> config
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(401);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            mapper.writeValue(response.getOutputStream(), Map.of("message", "Vui long dang nhap."));
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(403);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            mapper.writeValue(response.getOutputStream(), Map.of("message",
                                    "Ban khong co quyen thuc hien thao tac nay."));
                        }))
                .logout(config -> config.logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true).clearAuthentication(true).deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)))
                .addFilterBefore(new RefreshAccountFilter(accounts), AnonymousAuthenticationFilter.class);
        return http.build();
    }
}
