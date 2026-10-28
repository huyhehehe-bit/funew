package com.funews.backend;

import com.funews.backend.entity.Account;
import com.funews.backend.entity.NewsArticle;
import com.funews.backend.repository.AccountRepository;
import com.funews.backend.repository.CategoryRepository;
import com.funews.backend.repository.NewsArticleRepository;
import com.funews.backend.security.AccountPrincipal;
import com.funews.backend.service.NewsService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
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
 * Huy - luong bien tap cua Module 1.
 *
 * Dang nhap gia lap bang user(...) cua spring-security-test: dat san
 * AccountPrincipal vao SecurityContext, khong qua JWT. Luong JWT that da co
 * AuthJwtIntegrationTest cua QA kiem.
 *
 * Tai khoan mau: a@ la EDITOR, b@ va c@ la CONTRIBUTOR, reader@ la READER,
 * admin@ la ADMIN.
 */
class NewsIntegrationTest extends BackendIntegrationSupport {
    @Autowired
    private AccountRepository accounts;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private NewsArticleRepository articles;

    @Autowired
    private NewsService service;

    @Test
    void khachVangLaiChiThayBaiDaXuatBanVaKhongGhiDuoc() throws Exception {
        var shown = body(mvc.perform(get("/api/news")).andExpect(status().isOk()).andReturn());
        assertThat(shown).isNotEmpty();
        shown.forEach(article -> assertThat(article.path("status").asText()).isEqualTo("PUBLISHED"));

        mvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/news").contentType(MediaType.APPLICATION_JSON)
                        .content(json(draft("Khach tao bai")))).andExpect(status().isUnauthorized());
    }

    @Test
    void banNhapChiTacGiaVaBienTapThay() throws Exception {
        long id = create(contributor(), draft("Ban nhap rieng"));

        assertThat(ids(list(contributor()))).contains(id);
        // Tac gia khac khong thay, va cung khong doc duoc truc tiep.
        assertThat(ids(list(otherContributor()))).doesNotContain(id);
        mvc.perform(get("/api/news/" + id).with(otherContributor())).andExpect(status().isNotFound());
        // Doc gia cung vay.
        assertThat(ids(list(reader()))).doesNotContain(id);
        // Bien tap thi thay het - ho phai duyet bai.
        assertThat(ids(list(editor()))).contains(id);
    }

    @Test
    void serverTuGanTacGiaVaKhongCho_PUT_doi_trang_thai() throws Exception {
        Account author = account("b@fu.edu.vn");
        Map<String, Object> payload = draft("  Bai viet moi  ");
        // Client gui kem tac gia gia mao va co y dat trang thai PUBLISHED.
        payload.put("authorId", account("a@fu.edu.vn").getId());

        var created = body(mvc.perform(post("/api/news").with(contributor())
                        .contentType(MediaType.APPLICATION_JSON).content(json(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Bai viet moi"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn());
        assertThat(created.path("authorId").asLong()).isEqualTo(author.getId());
        assertThat(created.path("slug").asText()).isEqualTo("bai-viet-moi");

        // Tao bai thang len PUBLISHED: bi chan tu DTO/service - FR-ED-001.
        Map<String, Object> published = draft("Tu dang luon");
        published.put("status", "PUBLISHED");
        mvc.perform(post("/api/news").with(contributor())
                        .contentType(MediaType.APPLICATION_JSON).content(json(published)))
                .andExpect(status().isBadRequest());

        // Sua bai cung khong doi duoc trang thai.
        Map<String, Object> edit = draft("Doi trang thai bang PUT");
        edit.put("status", "PUBLISHED");
        mvc.perform(put("/api/news/" + created.path("id").asLong()).with(contributor())
                        .contentType(MediaType.APPLICATION_JSON).content(json(edit)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nopBaiRoiThiKhoaSua() throws Exception {
        long id = create(contributor(), draft("Bai se nop"));

        mvc.perform(post("/api/news/" + id + "/submit").with(contributor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.submittedAt").isNotEmpty());

        // AC-ED-001: nop roi la read-only voi tac gia.
        Map<String, Object> edit = draft("Sua sau khi nop");
        edit.put("status", "PENDING_REVIEW");
        mvc.perform(put("/api/news/" + id).with(contributor())
                        .contentType(MediaType.APPLICATION_JSON).content(json(edit)))
                .andExpect(status().isConflict());
    }

    @Test
    void traBaiLaiBatBuocCoLyDoVaTacGiaNhanThongBao() throws Exception {
        long id = create(contributor(), draft("Bai bi tra lai"));
        mvc.perform(post("/api/news/" + id + "/submit").with(contributor())).andExpect(status().isOk());

        // FR-ED-002: thieu ly do thi khong tra bai duoc.
        mvc.perform(post("/api/news/" + id + "/request-changes").with(editor())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("note", "   "))))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/news/" + id + "/request-changes").with(editor())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("note", "Bo sung danh sach nganh"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHANGES_REQUESTED"))
                .andExpect(jsonPath("$.reviewerId").value(account("a@fu.edu.vn").getId()));

        // FR-ED-004: nhat ky ghi du hai buoc.
        var trail = body(mvc.perform(get("/api/news/" + id + "/audit").with(contributor()))
                .andExpect(status().isOk()).andReturn());
        assertThat(actions(trail)).containsExactlyInAnyOrder("SUBMIT", "REQUEST_CHANGES");

        // AC-ED-002: ly do den tay tac gia.
        var inbox = body(mvc.perform(get("/api/notifications").with(contributor()))
                .andExpect(status().isOk()).andReturn());
        assertThat(inbox.path(0).path("type").asText()).isEqualTo("CHANGES_REQUESTED");
        assertThat(inbox.path(0).path("message").asText()).contains("Bo sung danh sach nganh");

        // Tra lai roi thi sua tiep va nop lai duoc.
        Map<String, Object> edit = draft("Bai da sua theo gop y");
        edit.put("status", "CHANGES_REQUESTED");
        mvc.perform(put("/api/news/" + id).with(contributor())
                        .contentType(MediaType.APPLICATION_JSON).content(json(edit)))
                .andExpect(status().isOk());
    }

    @Test
    void chiBienTapVaQuanTriMoiDuyetVaDangDuocBai() throws Exception {
        long id = create(contributor(), draft("Bai cho duyet"));
        mvc.perform(post("/api/news/" + id + "/submit").with(contributor())).andExpect(status().isOk());

        // NFR-ED-003: tac gia khong tu duyet bai cua minh.
        mvc.perform(post("/api/news/" + id + "/approve").with(contributor()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/news/" + id + "/approve").with(reader()))
                .andExpect(status().isForbidden());

        // Duyet khong kem gio hen: dang ngay.
        mvc.perform(post("/api/news/" + id + "/approve").with(editor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedAt").isNotEmpty());

        var trail = body(mvc.perform(get("/api/news/" + id + "/audit").with(editor()))
                .andExpect(status().isOk()).andReturn());
        assertThat(actions(trail)).containsExactlyInAnyOrder("SUBMIT", "APPROVE", "PUBLISH");

        // Bai da dang thi khach vang lai thay duoc.
        assertThat(ids(list(null))).contains(id);

        // Go bai: khoi trang cong khai nhung van con trong database.
        mvc.perform(post("/api/news/" + id + "/archive").with(editor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
        assertThat(ids(list(null))).doesNotContain(id);
        assertThat(articles.findById(id)).isPresent();
    }

    @Test
    void henGioThiTacVuNenDangBaiDuoiTenTaiKhoanSystem() throws Exception {
        long id = create(contributor(), draft("Bai hen gio"));
        mvc.perform(post("/api/news/" + id + "/submit").with(contributor())).andExpect(status().isOk());
        mvc.perform(post("/api/news/" + id + "/approve").with(editor())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("scheduledPublishAt", LocalDateTime.now().plusDays(1).toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        // Chua den gio thi tac vu nen khong dang.
        assertThat(service.publishDueArticles()).isZero();

        // Keo gio hen ve qua khu roi chay lai tac vu - AC-ED-003.
        NewsArticle article = articles.findById(id).orElseThrow();
        article.setScheduledPublishAt(LocalDateTime.now().minusMinutes(1));
        articles.save(article);

        assertThat(service.publishDueArticles()).isEqualTo(1);
        assertThat(articles.findById(id).orElseThrow().getStatus().name()).isEqualTo("PUBLISHED");

        var trail = body(mvc.perform(get("/api/news/" + id + "/audit").with(editor()))
                .andExpect(status().isOk()).andReturn());
        long systemId = account("system@fu.edu.vn").getId();
        assertThat(trail).anySatisfy(row -> {
            assertThat(row.path("action").asText()).isEqualTo("PUBLISH");
            assertThat(row.path("actorId").asLong()).isEqualTo(systemId);
        });
    }

    @Test
    void xoaBaiLaXoaMemVaDashboardDemTheoTrangThai() throws Exception {
        var before = body(mvc.perform(get("/api/dashboard").with(editor()))
                .andExpect(status().isOk()).andReturn());
        long pendingBefore = before.path("pendingReviewCount").asLong();

        long id = create(contributor(), draft("Bai se xoa"));
        mvc.perform(post("/api/news/" + id + "/submit").with(contributor())).andExpect(status().isOk());

        var after = body(mvc.perform(get("/api/dashboard").with(editor()))
                .andExpect(status().isOk()).andReturn());
        assertThat(after.path("pendingReviewCount").asLong()).isEqualTo(pendingBefore + 1);
        assertThat(after.path("recentNews").size()).isLessThanOrEqualTo(5);

        // Dang cho duyet thi tac gia khong xoa duoc... nhung ADMIN ghi de duoc.
        mvc.perform(delete("/api/news/" + id).with(admin())).andExpect(status().isNoContent());
        mvc.perform(get("/api/news/" + id).with(editor())).andExpect(status().isNotFound());
        // Xoa mem: dong van con, chi la co co deleted.
        assertThat(articles.findById(id).orElseThrow().isDeleted()).isTrue();
        assertThat(articles.findAllVisible().stream().map(NewsArticle::getId)).doesNotContain(id);
    }

    /* ---------- tien ich ---------- */

    private long create(RequestPostProcessor who, Map<String, Object> payload) throws Exception {
        return body(mvc.perform(post("/api/news").with(who)
                        .contentType(MediaType.APPLICATION_JSON).content(json(payload)))
                .andExpect(status().isCreated()).andReturn()).path("id").asLong();
    }

    private com.fasterxml.jackson.databind.JsonNode list(RequestPostProcessor who) throws Exception {
        var request = get("/api/news");
        if (who != null) {
            request = request.with(who);
        }
        return body(mvc.perform(request).andExpect(status().isOk()).andReturn());
    }

    private List<Long> ids(com.fasterxml.jackson.databind.JsonNode articles) {
        return java.util.stream.StreamSupport.stream(articles.spliterator(), false)
                .map(node -> node.path("id").asLong()).toList();
    }

    private List<String> actions(com.fasterxml.jackson.databind.JsonNode trail) {
        return java.util.stream.StreamSupport.stream(trail.spliterator(), false)
                .map(node -> node.path("action").asText()).toList();
    }

    /** HashMap chu khong Map.of: con cho test put thenm truong vao payload. */
    private Map<String, Object> draft(String title) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", title);
        payload.put("summary", "Tom tat");
        payload.put("content", "Noi dung");
        payload.put("source", "Kiem thu");
        payload.put("categoryId", categoryId());
        payload.put("tags", List.of("kiem thu", "module 1"));
        payload.put("status", "DRAFT");
        return payload;
    }

    private Account account(String email) {
        return accounts.findByEmailIgnoreCase(email).orElseThrow();
    }

    private RequestPostProcessor as(String email) {
        return user(AccountPrincipal.from(account(email)));
    }

    private RequestPostProcessor admin() { return as("admin@fu.edu.vn"); }

    private RequestPostProcessor editor() { return as("a@fu.edu.vn"); }

    private RequestPostProcessor contributor() { return as("b@fu.edu.vn"); }

    private RequestPostProcessor otherContributor() { return as("c@fu.edu.vn"); }

    private RequestPostProcessor reader() { return as("reader@fu.edu.vn"); }

    private long categoryId() {
        return categories.findAllByOrderByIdAsc().stream().findFirst().orElseThrow().getId();
    }
}
