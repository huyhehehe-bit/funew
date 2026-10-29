package com.funews.backend;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * QA - khach tu dang ky tai khoan doc gia.
 *
 * Diem quan trong nhat o day khong phai "dang ky duoc" ma la "dang ky xong van
 * chi la READER": neu client ep duoc vai tro thi ai cung tu dang ky lam EDITOR
 * roi tu duyet bai cua minh.
 */
class RegisterIntegrationTest extends BackendIntegrationSupport {
    @Test
    void dangKyXongVaoDuocNgayVaChiLaDocGia() throws Exception {
        var created = body(mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(dangKy("doc-gia-moi@fu.edu.vn", "doc-gia-moi"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.roleCode").value("READER"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andReturn());

        // Token tra ve dung duoc ngay, khong phai dang nhap lai.
        String token = created.path("accessToken").asText();
        mvc.perform(get("/api/auth/me").with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("doc-gia-moi@fu.edu.vn"));

        // Va dang nhap binh thuong cung duoc.
        assertThat(currentUserId(token(("doc-gia-moi@fu.edu.vn"), "mat-khau-123")))
                .isEqualTo(created.path("user").path("id").asLong());

        // Doc gia khong duoc them quyen gi.
        mvc.perform(get("/api/accounts").with(bearer(token))).andExpect(status().isForbidden());
        mvc.perform(get("/api/news/review-queue").with(bearer(token))).andExpect(status().isForbidden());
    }

    @Test
    void epVaiTroQuaBodyThiBiBoQua() throws Exception {
        Map<String, Object> payload = dangKy("ke-ep-quyen@fu.edu.vn", "ke-ep-quyen");
        payload.put("roleId", 1);
        payload.put("roleCode", "ADMIN");
        payload.put("status", 1);

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(json(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.roleCode").value("READER"));
    }

    @Test
    void trungEmailHoacUsernameThiBiTuChoi() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(dangKy("nguoi-dau-tien@fu.edu.vn", "nguoi-dau-tien"))))
                .andExpect(status().isCreated());

        // Trung email, khac hoa thuong van tinh la trung.
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(dangKy("NGUOI-DAU-TIEN@FU.EDU.VN", "ten-khac"))))
                .andExpect(status().isConflict());

        // Trung username.
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(dangKy("email-khac@fu.edu.vn", "nguoi-dau-tien"))))
                .andExpect(status().isConflict());

        // Trung voi tai khoan co san tu truoc cung vay.
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(dangKy("a@fu.edu.vn", "ten-moi-toanh"))))
                .andExpect(status().isConflict());
    }

    @Test
    void duLieuSaiBiTuChoi() throws Exception {
        Map<String, Object> saiEmail = dangKy("khong-phai-email", "ten-hop-le");
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(json(saiEmail)))
                .andExpect(status().isBadRequest());

        Map<String, Object> matKhauNgan = dangKy("mat-khau-ngan@fu.edu.vn", "mat-khau-ngan");
        matKhauNgan.put("password", "123");
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(json(matKhauNgan)))
                .andExpect(status().isBadRequest());

        Map<String, Object> thieuTen = dangKy("thieu-ten@fu.edu.vn", "thieu-ten");
        thieuTen.put("fullName", "   ");
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(json(thieuTen)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deTrongUsernameThiLayEmail() throws Exception {
        Map<String, Object> payload = dangKy("khong-dat-username@fu.edu.vn", null);
        payload.remove("username");
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(json(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.username").value("khong-dat-username@fu.edu.vn"));
    }

    private Map<String, Object> dangKy(String email, String username) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("fullName", "Doc gia dang ky");
        payload.put("email", email);
        if (username != null) {
            payload.put("username", username);
        }
        payload.put("password", "mat-khau-123");
        return payload;
    }
}
