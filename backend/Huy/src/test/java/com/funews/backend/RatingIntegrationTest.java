package com.funews.backend;

import com.funews.backend.entity.NewsArticle;
import com.funews.backend.repository.AccountRepository;
import com.funews.backend.repository.CategoryRepository;
import com.funews.backend.repository.NewsArticleRepository;
import com.funews.backend.security.AccountPrincipal;
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
 * Huy - cham diem 1-5 sao.
 *
 * Khach vang lai doc bai duoc nhung cham diem thi phai dang nhap: do la ranh
 * gioi giua khach va doc gia da dang ky.
 *
 * ratingAvg tren H2 luon null vi cot do chi la cot tinh toan tren SQL Server,
 * nen o day kiem ratingCount va ratingSum.
 */
class RatingIntegrationTest extends BackendIntegrationSupport {
    private static final String DA_DANG = "thong-bao-tuyen-sinh-2026";

    @Autowired
    private AccountRepository accounts;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private NewsArticleRepository articles;

    @Test
    void khachVangLaiKhongChamDiemDuoc() throws Exception {
        long id = daDang().getId();
        mvc.perform(put("/api/news/" + id + "/rating")
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("score", 5))))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/news/" + id + "/rating")).andExpect(status().isUnauthorized());

        // Nhung doc bai thi van duoc, va van thay diem trung binh.
        mvc.perform(get("/api/news/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ratingCount").value(1));
    }

    @Test
    void chamDiemRoiDoiDiemRoiGoDiem() throws Exception {
        NewsArticle article = daDang();
        long id = article.getId();
        long luotTruoc = article.getRatingCount();
        long tongTruoc = article.getRatingSum();

        // Nguoi moi cham diem: so luot tang 1.
        mvc.perform(put("/api/news/" + id + "/rating").with(contributor())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("score", 4))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(4))
                .andExpect(jsonPath("$.ratingCount").value((int) luotTruoc + 1));
        assertThat(articles.findById(id).orElseThrow().getRatingSum()).isEqualTo(tongTruoc + 4);

        // Cham lai: SUA diem cu, so luot khong tang them.
        mvc.perform(put("/api/news/" + id + "/rating").with(contributor())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("score", 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ratingCount").value((int) luotTruoc + 1));
        assertThat(articles.findById(id).orElseThrow().getRatingSum()).isEqualTo(tongTruoc + 2);

        // Doc lai diem cua chinh minh.
        mvc.perform(get("/api/news/" + id + "/rating").with(contributor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(2));

        // Go diem: tru ca so luot lan tong.
        mvc.perform(delete("/api/news/" + id + "/rating").with(contributor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").doesNotExist())
                .andExpect(jsonPath("$.ratingCount").value((int) luotTruoc));
        assertThat(articles.findById(id).orElseThrow().getRatingSum()).isEqualTo(tongTruoc);

        // Go lan nua thi khong con gi de go.
        mvc.perform(delete("/api/news/" + id + "/rating").with(contributor()))
                .andExpect(status().isNotFound());
    }

    @Test
    void moiNguoiMotDiemVaDiemCuaNguoiKhacKhongBiDe() throws Exception {
        long id = daDang().getId();
        // reader@ da cham 5 diem san trong du lieu mau.
        mvc.perform(get("/api/news/" + id + "/rating").with(reader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(5));

        mvc.perform(put("/api/news/" + id + "/rating").with(contributor())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("score", 1))))
                .andExpect(status().isOk());

        // Diem cua reader@ van nguyen.
        mvc.perform(get("/api/news/" + id + "/rating").with(reader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(5));
    }

    @Test
    void chiChamDuocBaiDangCongKhaiVaDiemPhaiTu1Den5() throws Exception {
        long id = daDang().getId();
        for (int score : new int[]{0, 6, -1}) {
            mvc.perform(put("/api/news/" + id + "/rating").with(reader())
                            .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("score", score))))
                    .andExpect(status().isBadRequest());
        }

        // Bai chua xuat ban thi khong cham duoc.
        long banNhap = body(mvc.perform(post("/api/news").with(contributor())
                        .contentType(MediaType.APPLICATION_JSON).content(json(banNhap())))
                .andExpect(status().isCreated()).andReturn()).path("id").asLong();
        mvc.perform(put("/api/news/" + banNhap + "/rating").with(reader())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("score", 5))))
                .andExpect(status().isConflict());

        mvc.perform(put("/api/news/999999/rating").with(reader())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("score", 5))))
                .andExpect(status().isNotFound());
    }

    private NewsArticle daDang() {
        return articles.findBySlugAndDeletedFalse(DA_DANG).orElseThrow();
    }

    private Map<String, Object> banNhap() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", "Ban nhap khong cham diem duoc");
        payload.put("summary", "Tom tat");
        payload.put("content", "Noi dung");
        payload.put("source", "Kiem thu");
        payload.put("categoryId", categories.findAllByOrderByIdAsc().get(0).getId());
        payload.put("tags", List.of("kiem thu"));
        payload.put("status", "DRAFT");
        return payload;
    }

    private RequestPostProcessor as(String email) {
        return user(AccountPrincipal.from(accounts.findByEmailIgnoreCase(email).orElseThrow()));
    }

    private RequestPostProcessor reader() { return as("reader@fu.edu.vn"); }

    private RequestPostProcessor contributor() { return as("b@fu.edu.vn"); }
}
