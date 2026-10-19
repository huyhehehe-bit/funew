package com.funews.backend;

import com.funews.backend.entity.Account;
import com.funews.backend.repository.AccountRepository;
import com.funews.backend.repository.CategoryRepository;
import com.funews.backend.security.AccountPrincipal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Huy - kiem thu bai viet va dashboard.
 *
 * Dang nhap duoc gia lap bang user(...) cua spring-security-test, chua goi
 * POST /api/auth/login vi AuthController cua QA chua viet. Khi QA viet xong,
 * doi sang login() cua BackendIntegrationSupport de kiem tra ca luong phien that.
 */
class NewsIntegrationTest extends BackendIntegrationSupport {
    @Autowired
    private AccountRepository accounts;

    @Autowired
    private CategoryRepository categories;

    @Test
    void chuaDangNhapVanDocDuocTinTucNhungKhongGhiDuoc() throws Exception {
        // Doc bai viet mo cong khai cho khach vang lai.
        mvc.perform(get("/api/news")).andExpect(status().isOk());

        // Nhung dashboard van phai dang nhap.
        mvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());

        // Va mo GET khong co nghia la mo ghi: tao bai van bi chan.
        mvc.perform(post("/api/news")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(news("Khach tao bai", categoryId(), 1))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void taoBaiVietThiServerTuGanTacGiaVaNgayTao() throws Exception {
        Account author = staffAccount();
        long otherId = accounts.findByEmailIgnoreCase("a@fu.edu.vn").orElseThrow().getId();

        // Client co gui kem createdById va createdDate gia, server phai bo qua.
        Map<String, Object> payload = Map.of("title", "  Bai viet moi  ", "headline", "Tom tat",
                "content", "Noi dung chi tiet", "source", "Phong CTSV", "categoryId", categoryId(),
                "tags", "test", "status", 1, "createdById", otherId, "createdDate", "2000-01-01");

        var created = body(mvc.perform(post("/api/news").with(staff())
                        .contentType(MediaType.APPLICATION_JSON).content(json(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Bai viet moi"))
                .andReturn());

        assertThat(created.path("createdById").asLong()).isEqualTo(author.getId());
        assertThat(created.path("createdDate").asText())
                .isEqualTo(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).toString());
    }

    @Test
    void suaBaiGiuNguyenTacGiaVaNgayTao() throws Exception {
        long id = createNews("Bai goc");
        var truoc = body(mvc.perform(get("/api/news/" + id).with(staff())).andReturn());

        var sau = body(mvc.perform(put("/api/news/" + id).with(staff())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(news("Bai da sua", categoryId(), 0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Bai da sua"))
                .andExpect(jsonPath("$.status").value(0))
                .andReturn());

        assertThat(sau.path("createdById").asLong()).isEqualTo(truoc.path("createdById").asLong());
        assertThat(sau.path("createdDate").asText()).isEqualTo(truoc.path("createdDate").asText());
    }

    @Test
    void xoaBaiThiKhongCon() throws Exception {
        long id = createNews("Bai se xoa");

        mvc.perform(delete("/api/news/" + id).with(staff())).andExpect(status().isNoContent());
        mvc.perform(get("/api/news/" + id).with(staff())).andExpect(status().isNotFound());
    }

    @Test
    void duLieuSaiBiTuChoi() throws Exception {
        // Tieu de rong -> 400 do @NotBlank trong NewsRequest.
        mvc.perform(post("/api/news").with(staff())
                        .contentType(MediaType.APPLICATION_JSON).content(json(news("   ", categoryId(), 1))))
                .andExpect(status().isBadRequest());

        // status ngoai khoang 0..1 -> 400.
        mvc.perform(post("/api/news").with(staff())
                        .contentType(MediaType.APPLICATION_JSON).content(json(news("Status sai", categoryId(), 5))))
                .andExpect(status().isBadRequest());

        // Danh muc khong ton tai -> 404 do NewsService nem ApiException.
        mvc.perform(post("/api/news").with(staff())
                        .contentType(MediaType.APPLICATION_JSON).content(json(news("Danh muc la", 999999L, 1))))
                .andExpect(status().isNotFound());
    }

    @Test
    void dashboardDemDungVaTraToiDaNamBaiMoiNhat() throws Exception {
        var truoc = body(mvc.perform(get("/api/dashboard").with(staff())).andExpect(status().isOk()).andReturn());
        long soBai = truoc.path("newsCount").asLong();
        long soBaiHoatDong = truoc.path("activeNewsCount").asLong();

        createNews("Bai cho dashboard");

        var sau = body(mvc.perform(get("/api/dashboard").with(staff())).andExpect(status().isOk()).andReturn());
        assertThat(sau.path("accountCount").asLong()).isEqualTo(accounts.count());
        assertThat(sau.path("categoryCount").asLong()).isEqualTo(categories.count());
        assertThat(sau.path("newsCount").asLong()).isEqualTo(soBai + 1);
        assertThat(sau.path("activeNewsCount").asLong()).isEqualTo(soBaiHoatDong + 1);
        assertThat(sau.path("recentNews").size()).isLessThanOrEqualTo(5);
    }

    private long createNews(String title) throws Exception {
        return body(mvc.perform(post("/api/news").with(staff())
                        .contentType(MediaType.APPLICATION_JSON).content(json(news(title, categoryId(), 1))))
                .andExpect(status().isCreated()).andReturn()).path("id").asLong();
    }

    private Map<String, Object> news(String title, long categoryId, int status) {
        return Map.of("title", title, "headline", "Tom tat", "content", "Noi dung",
                "source", "Kiem thu", "categoryId", categoryId, "tags", "test", "status", status);
    }

    private Account staffAccount() {
        return accounts.findByEmailIgnoreCase("b@fu.edu.vn").orElseThrow();
    }

    private RequestPostProcessor staff() {
        return user(AccountPrincipal.from(staffAccount()));
    }

    private long categoryId() {
        return categories.findAll().stream().findFirst().orElseThrow().getId();
    }
}
