package com.funews.backend;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Long - loc danh muc theo trang thai: GET /api/categories?status=true|false.
 *
 * isActive la boolean chu khong con 0/1, vi cot is_active trong database da doi
 * tu INT sang BIT.
 */
class CategoryFilterIntegrationTest extends BackendIntegrationSupport {
    @Test
    void khongCoThamSoThiTraVeTatCa() throws Exception {
        String admin = token("admin@fu.edu.vn", "Admin");
        long tatThu = taoDanhMuc(admin, "Danh muc da tat", false);

        List<Long> tatCa = ids(categories(null));
        assertThat(tatCa).contains(tatThu);
        assertThat(tatCa).hasSizeGreaterThanOrEqualTo(4);
    }

    @Test
    void locDuocDanhMucDangBatVaDangTat() throws Exception {
        String admin = token("admin@fu.edu.vn", "Admin");
        long dangBat = taoDanhMuc(admin, "Danh muc dang bat", true);
        long daTat = taoDanhMuc(admin, "Danh muc da tat", false);

        JsonNode bat = categories(true);
        assertThat(ids(bat)).contains(dangBat).doesNotContain(daTat);
        bat.forEach(node -> assertThat(node.path("isActive").asBoolean()).isTrue());

        JsonNode tat = categories(false);
        assertThat(ids(tat)).contains(daTat).doesNotContain(dangBat);
        tat.forEach(node -> assertThat(node.path("isActive").asBoolean()).isFalse());
    }

    @Test
    void khachVangLaiCungLocDuoc() throws Exception {
        // GET /api/categories mo cong khai, nen tham so loc cung phai dung duoc
        // khi chua dang nhap.
        mvc.perform(get("/api/categories").param("status", "true")).andExpect(status().isOk());
        // Tham so khong phai boolean thi tra 400 chu khong 500.
        mvc.perform(get("/api/categories").param("status", "khong-phai-boolean"))
                .andExpect(status().isBadRequest());
    }

    private JsonNode categories(Boolean status) throws Exception {
        var request = get("/api/categories");
        if (status != null) {
            request = request.param("status", status.toString());
        }
        return body(mvc.perform(request).andExpect(status().isOk()).andReturn());
    }

    private long taoDanhMuc(String token, String name, boolean active) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", name);
        payload.put("description", "Kiem thu loc theo trang thai");
        payload.put("parentId", 0);
        payload.put("isActive", active);
        return body(mvc.perform(post("/api/categories").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json(payload)))
                .andExpect(status().isCreated()).andReturn()).path("id").asLong();
    }

    private List<Long> ids(JsonNode rows) {
        return StreamSupport.stream(rows.spliterator(), false).map(row -> row.path("id").asLong()).toList();
    }
}
