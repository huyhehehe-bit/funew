package com.funews.backend;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * QA - xac thuc bang JWT Bearer va phan quyen theo vai tro.
 *
 * Ten class giu nguyen de khong lam roi lich su git, nhung khong con phien nao
 * ca: SecurityConfig da dat SessionCreationPolicy.STATELESS va
 * NullSecurityContextRepository, nen chi header Authorization moi xac thuc duoc.
 *
 * Vai tro cua du lieu mau: admin@ la ADMIN, a@ la EDITOR, b@ va c@ la
 * CONTRIBUTOR, reader@ la READER.
 */
class AuthSessionIntegrationTest extends BackendIntegrationSupport {
    @Test
    void clientCanLoginUseTokenAndLogout() throws Exception {
        MvcResult loginResponse = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "Admin", "password", "Admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andExpect(jsonPath("$.user.email").value("admin@fu.edu.vn"))
                .andExpect(jsonPath("$.user.roleCode").value("ADMIN"))
                // Khong bao gio tra mat khau hay hash, ke ca long trong user.
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                // Khong con cookie phien nao: token nam trong body, client tu giu.
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andReturn();
        String token = body(loginResponse).path("accessToken").asText();

        mvc.perform(get("/api/auth/me").with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(body(loginResponse).path("user").path("id").asInt()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        mvc.perform(post("/api/auth/logout").with(bearer(token)))
                .andExpect(status().isNoContent());

        // Dang xuat la thu hoi token do: dung lai chinh token cu phai bi tu choi.
        mvc.perform(get("/api/auth/me").with(bearer(token)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidCredentialsAndUnauthenticatedApiRequests() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "a@fu.edu.vn", "password", "wrong-password"))))
                .andExpect(status().isUnauthorized());
        // Token rac cung bi tu choi, va tra kem WWW-Authenticate theo dung chuan Bearer.
        mvc.perform(get("/api/auth/me").with(bearer("khong-phai-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"));
        // Van phai dang nhap moi xem duoc.
        for (String endpoint : new String[]{"/api/auth/me", "/api/accounts", "/api/profile", "/api/dashboard",
                "/api/notifications", "/api/news/review-queue"}) {
            mvc.perform(get(endpoint)).andExpect(status().isUnauthorized());
        }
        // Doc cong khai: khach chua dang nhap van xem duoc tin tuc, danh muc va ten tac gia.
        for (String endpoint : new String[]{"/api/news", "/api/categories", "/api/accounts/lookup"}) {
            mvc.perform(get(endpoint)).andExpect(status().isOk());
        }
    }

    @Test
    void allowsViteCorsPreflightAndRejectsUnknownOrigins() throws Exception {
        mvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                // Bearer thi khong dung cookie, nen CORS KHONG duoc phep gui credentials.
                // Truoc day header nay la true vi phien nam trong cookie JSESSIONID.
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"))
                .andExpect(result -> assertThat(result.getResponse().getHeader("Access-Control-Allow-Headers"))
                        .containsIgnoringCase("authorization"));

        mvc.perform(options("/api/auth/login")
                        .header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void contributorCanReadNewsAndAuthorNamesButCannotManageAccountsOrCategories() throws Exception {
        String contributor = token("b@fu.edu.vn", "123");
        mvc.perform(get("/api/news").with(bearer(contributor))).andExpect(status().isOk());
        MvcResult categories = mvc.perform(get("/api/categories").with(bearer(contributor)))
                .andExpect(status().isOk()).andReturn();
        long categoryId = body(categories).get(0).path("id").asLong();

        mvc.perform(get("/api/accounts/lookup").with(bearer(contributor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").isNotEmpty())
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());

        // Quan ly tai khoan va danh muc: chi ADMIN.
        mvc.perform(get("/api/accounts").with(bearer(contributor))).andExpect(status().isForbidden());
        mvc.perform(post("/api/accounts").with(bearer(contributor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(account("Forbidden", "forbidden@fu.edu.vn", "123", 1))))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/accounts/{id}", currentUserId(contributor)).with(bearer(contributor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(account("Forbidden", "b@fu.edu.vn", "123", 1))))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/accounts/{id}", currentUserId(contributor)).with(bearer(contributor)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/categories").with(bearer(contributor))
                        .contentType(MediaType.APPLICATION_JSON).content(json(category("Forbidden", 0))))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/categories/{id}", categoryId).with(bearer(contributor))
                        .contentType(MediaType.APPLICATION_JSON).content(json(category("Forbidden", 0))))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/categories/{id}", categoryId).with(bearer(contributor)))
                .andExpect(status().isForbidden());

        // NFR-ED-003: duyet va dang bai khong phai viec cua cong tac vien.
        mvc.perform(get("/api/news/review-queue").with(bearer(contributor))).andExpect(status().isForbidden());
    }

    @Test
    void newsCrudUsesAuthenticatedAuthorAndProtectsReferencedCategory() throws Exception {
        String admin = token("admin@fu.edu.vn", "Admin");
        String contributor = token("b@fu.edu.vn", "123");
        long contributorId = currentUserId(contributor);
        long categoryId = createCategory(admin, "Fixture news category", 0);

        // Client gui kem authorId gia mao, server phai bo qua.
        Map<String, Object> payload = draft("Fixture article", categoryId);
        payload.put("authorId", currentUserId(admin));
        MvcResult created = mvc.perform(post("/api/news").with(bearer(contributor))
                        .contentType(MediaType.APPLICATION_JSON).content(json(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorId").value((int) contributorId))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn();
        long articleId = body(created).path("id").asLong();

        mvc.perform(put("/api/news/{id}", articleId).with(bearer(contributor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(draft("Updated article", categoryId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated article"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.authorId").value((int) contributorId));

        JsonNode persisted = findById(body(mvc.perform(get("/api/news").with(bearer(contributor)))
                .andExpect(status().isOk()).andReturn()), articleId);
        assertThat(persisted).isNotNull();
        assertThat(persisted.path("title").asText()).isEqualTo("Updated article");
        assertThat(persisted.path("categoryId").asLong()).isEqualTo(categoryId);

        // Danh muc dang co bai thi khong xoa duoc.
        mvc.perform(delete("/api/categories/{id}", categoryId).with(bearer(admin)))
                .andExpect(status().isConflict());

        mvc.perform(delete("/api/news/{id}", articleId).with(bearer(contributor)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/news/{id}", articleId).with(bearer(contributor)))
                .andExpect(status().isNotFound());

        // Bai da xoa mem khong con giu danh muc nua.
        mvc.perform(delete("/api/categories/{id}", categoryId).with(bearer(admin)))
                .andExpect(status().isNoContent());
    }

    /** Payload bai viet theo hop dong moi: summary, tags dang danh sach, status dang chuoi. */
    private Map<String, Object> draft(String title, long categoryId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", title);
        payload.put("summary", "Fixture summary");
        payload.put("content", "Fixture article content");
        payload.put("source", "Integration test");
        payload.put("categoryId", categoryId);
        payload.put("tags", List.of("fixture", "integration"));
        payload.put("status", "DRAFT");
        return payload;
    }
}
