package com.funews.backend;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthPasswordIntegrationTest extends JwtIntegrationSupport {
    private static final String PASSWORD = "\u00e9".repeat(36);

    @Test
    void unicodePasswordAt72Utf8BytesCanLoginAndUseJwt() throws Exception {
        account.setPasswordHash(encoder.encode(PASSWORD));
        String token = login(account.getEmail(), PASSWORD);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(account.getEmail()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void passwordBeyond72Utf8BytesCannotAuthenticateWithValidPrefix() throws Exception {
        account.setPasswordHash(encoder.encode(PASSWORD));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", account.getEmail(), "password", PASSWORD + "x"))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    void incorrectPasswordRemainsUnauthorized() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", account.getEmail(), "password", "incorrect"))))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.accessToken").doesNotExist());
    }
}
