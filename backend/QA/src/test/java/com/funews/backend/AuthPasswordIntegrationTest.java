package com.funews.backend;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthPasswordIntegrationTest extends BackendIntegrationSupport {
    private static final String EMAIL = "unicode-password@fu.edu.vn";
    private static final String PASSWORD = "\u00e9".repeat(36);

    @Test
    void unicodePasswordAt72Utf8BytesCanLoginAndRestoreSession() throws Exception {
        createUnicodePasswordAccount();

        MockHttpSession authenticated = login(EMAIL, PASSWORD);

        mvc.perform(get("/api/auth/me").session(authenticated))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.role").value(2))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void passwordBeyond72Utf8BytesCannotAuthenticateWithValidPrefix() throws Exception {
        createUnicodePasswordAccount();
        MockHttpSession anonymous = new MockHttpSession();

        // BCrypt accepts this suffix after a valid 72-byte prefix unless login validates bytes.
        mvc.perform(post("/api/auth/login").session(anonymous)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", EMAIL, "password", PASSWORD + "x"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());

        mvc.perform(get("/api/auth/me").session(anonymous))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void incorrectPasswordWithinByteLimitRemainsUnauthorized() throws Exception {
        createUnicodePasswordAccount();
        MockHttpSession anonymous = new MockHttpSession();

        mvc.perform(post("/api/auth/login").session(anonymous)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", EMAIL, "password", "incorrect-password"))))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/auth/me").session(anonymous))
                .andExpect(status().isUnauthorized());
    }

    private void createUnicodePasswordAccount() throws Exception {
        MockHttpSession admin = login("a@fu.edu.vn", "123");
        mvc.perform(post("/api/accounts").session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(account("Unicode password user", EMAIL, PASSWORD, 2))))
                .andExpect(status().isCreated());
    }
}
