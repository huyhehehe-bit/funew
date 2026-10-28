package com.funews.backend;

import com.funews.backend.entity.NewsArticle;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Quy tac bien tap - SRS muc 3.2.
 *
 * Moi test o day deu kiem CA HAI dau: sua duoc quy tac, VA quy tac do co hieu
 * luc that trong luong duyet bai. Cau hinh ma khong ai doc thi chi la trang tri.
 */
class EditorialRulesIntegrationTest extends BackendIntegrationSupport {
    @Autowired
    private com.funews.backend.repository.AccountRepository accounts;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private NewsArticleRepository articles;

    @Autowired
    private NewsService news;

    @Test
    void aiDangNhapCungDocDuocNhungChiAdminSuaDuoc() throws Exception {
        mvc.perform(get("/api/editorial-rules").with(contributor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autoPublishScheduled").value(true))
                .andExpect(jsonPath("$.minSummaryLength").value(0));
        mvc.perform(get("/api/editorial-rules")).andExpect(status().isUnauthorized());

        mvc.perform(put("/api/editorial-rules").with(editor())
                        .contentType(MediaType.APPLICATION_JSON).content(json(rules(true, false, 0))))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/editorial-rules").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(json(rules(false, true, 40))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autoPublishScheduled").value(false))
                .andExpect(jsonPath("$.requireNoteOnApprove").value(true))
                .andExpect(jsonPath("$.minSummaryLength").value(40))
                .andExpect(jsonPath("$.updatedById").value(account("admin@fu.edu.vn")));

        // Nguong vuot qua do dai cot summary thi bi tu choi.
        mvc.perform(put("/api/editorial-rules").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(json(rules(true, false, 2000))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tomTatNganHonNguongThiKhongNopDuoc() throws Exception {
        datQuyTac(true, false, 40);

        Map<String, Object> ngan = draft("Bai tom tat ngan");
        ngan.put("summary", "Qua ngan");
        // Luu nhap thi van duoc - quy tac chi chan luc NOP.
        long id = body(mvc.perform(post("/api/news").with(contributor())
                        .contentType(MediaType.APPLICATION_JSON).content(json(ngan)))
                .andExpect(status().isCreated()).andReturn()).path("id").asLong();
        mvc.perform(post("/api/news/" + id + "/submit").with(contributor()))
                .andExpect(status().isBadRequest());

        // Nop thang luc tao cung bi chan.
        Map<String, Object> nopLuon = draft("Nop luon nhung ngan");
        nopLuon.put("summary", "Van ngan");
        nopLuon.put("status", "PENDING_REVIEW");
        mvc.perform(post("/api/news").with(contributor())
                        .contentType(MediaType.APPLICATION_JSON).content(json(nopLuon)))
                .andExpect(status().isBadRequest());

        // Sua tom tat cho du dai thi nop duoc.
        Map<String, Object> dai = draft("Bai tom tat ngan");
        dai.put("summary", "Tom tat nay du dai de vuot qua nguong bon muoi ky tu da dat.");
        dai.put("status", "DRAFT");
        mvc.perform(put("/api/news/" + id).with(contributor())
                        .contentType(MediaType.APPLICATION_JSON).content(json(dai)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/news/" + id + "/submit").with(contributor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));
    }

    @Test
    void batQuyTacThiDuyetBaiPhaiGhiChu() throws Exception {
        datQuyTac(true, true, 0);
        long id = nopBai("Bai can ghi chu khi duyet");

        mvc.perform(post("/api/news/" + id + "/approve").with(editor()))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/news/" + id + "/approve").with(editor())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("note", "Dong y dang, noi dung on."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void tatTuDongDangThiBaiHenGioNamYen() throws Exception {
        datQuyTac(false, false, 0);
        long id = nopBai("Bai hen gio khi da tat tu dong dang");
        mvc.perform(post("/api/news/" + id + "/approve").with(editor())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("scheduledPublishAt", LocalDateTime.now().plusDays(1).toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        NewsArticle article = articles.findById(id).orElseThrow();
        article.setScheduledPublishAt(LocalDateTime.now().minusMinutes(5));
        articles.save(article);

        // Da den gio nhung quy tac dang tat: tac vu nen khong dang.
        assertThat(news.publishDueArticles()).isZero();
        assertThat(articles.findById(id).orElseThrow().getStatus().name()).isEqualTo("SCHEDULED");

        // Bat lai thi dang duoc.
        datQuyTac(true, false, 0);
        assertThat(news.publishDueArticles()).isEqualTo(1);
        assertThat(articles.findById(id).orElseThrow().getStatus().name()).isEqualTo("PUBLISHED");
    }

    /* ---------- tien ich ---------- */

    private void datQuyTac(boolean tuDongDang, boolean ghiChuKhiDuyet, int tomTatToiThieu) throws Exception {
        mvc.perform(put("/api/editorial-rules").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(rules(tuDongDang, ghiChuKhiDuyet, tomTatToiThieu))))
                .andExpect(status().isOk());
    }

    private long nopBai(String title) throws Exception {
        long id = body(mvc.perform(post("/api/news").with(contributor())
                        .contentType(MediaType.APPLICATION_JSON).content(json(draft(title))))
                .andExpect(status().isCreated()).andReturn()).path("id").asLong();
        mvc.perform(post("/api/news/" + id + "/submit").with(contributor())).andExpect(status().isOk());
        return id;
    }

    private Map<String, Object> rules(boolean tuDongDang, boolean ghiChuKhiDuyet, int tomTatToiThieu) {
        return Map.of("autoPublishScheduled", tuDongDang, "requireNoteOnApprove", ghiChuKhiDuyet,
                "minSummaryLength", tomTatToiThieu);
    }

    private Map<String, Object> draft(String title) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", title);
        payload.put("summary", "Tom tat du dai cho cac truong hop khong rang buoc do dai.");
        payload.put("content", "Noi dung");
        payload.put("source", "Kiem thu");
        payload.put("categoryId", categories.findAllByOrderByIdAsc().get(0).getId());
        payload.put("tags", List.of("quy tac"));
        payload.put("status", "DRAFT");
        return payload;
    }

    private long account(String email) {
        return accounts.findByEmailIgnoreCase(email).orElseThrow().getId();
    }

    private RequestPostProcessor as(String email) {
        return user(AccountPrincipal.from(accounts.findByEmailIgnoreCase(email).orElseThrow()));
    }

    private RequestPostProcessor admin() { return as("admin@fu.edu.vn"); }

    private RequestPostProcessor editor() { return as("a@fu.edu.vn"); }

    private RequestPostProcessor contributor() { return as("b@fu.edu.vn"); }
}
