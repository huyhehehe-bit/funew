package com.funews.backend.service;

import com.funews.backend.dto.ApiDtos.DashboardResponse;
import com.funews.backend.dto.ApiDtos.NewsRequest;
import com.funews.backend.dto.ApiDtos.NewsResponse;
import com.funews.backend.entity.Account;
import com.funews.backend.entity.Category;
import com.funews.backend.entity.NewsArticle;
import com.funews.backend.exception.ApiException;
import com.funews.backend.repository.AccountRepository;
import com.funews.backend.repository.CategoryRepository;
import com.funews.backend.repository.NewsArticleRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NewsService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final AccountRepository accounts;
    private final CategoryRepository categories;
    private final NewsArticleRepository news;

    public NewsService(AccountRepository accounts, CategoryRepository categories, NewsArticleRepository news) {
        this.accounts = accounts;
        this.categories = categories;
        this.news = news;
    }

    public List<NewsResponse> listNews() {
        return news.findAllByOrderByIdAsc().stream().map(this::newsResponse).toList();
    }

    public NewsResponse getNews(Long id) {
        return newsResponse(requireNews(id));
    }

    @Transactional
    public NewsResponse createNews(NewsRequest request, String currentEmail) {
        NewsArticle article = new NewsArticle();
        applyNews(article, request);
        // Tac gia va ngay tao do server quyet dinh; createdById/createdDate client gui len bi bo qua.
        article.setCreatedBy(requireCurrentAccount(currentEmail));
        article.setCreatedDate(LocalDate.now(ZONE));
        return newsResponse(news.save(article));
    }

    @Transactional
    public NewsResponse updateNews(Long id, NewsRequest request) {
        NewsArticle article = requireNews(id);
        // applyNews khong dung toi createdBy va createdDate nen sua bai van giu nguyen tac gia, ngay tao.
        applyNews(article, request);
        return newsResponse(news.save(article));
    }

    @Transactional
    public void deleteNews(Long id) {
        news.delete(requireNews(id));
    }

    public DashboardResponse dashboard() {
        return new DashboardResponse(accounts.count(), categories.count(), news.count(), news.countByStatus(1),
                news.findTop5ByOrderByCreatedDateDescIdDesc().stream().map(this::newsResponse).toList());
    }

    private void applyNews(NewsArticle article, NewsRequest request) {
        article.setTitle(request.title().trim());
        article.setHeadline(textOrEmpty(request.headline()));
        article.setContent(textOrEmpty(request.content()));
        article.setSource(textOrEmpty(request.source()));
        article.setCategory(requireCategory(request.categoryId()));
        article.setTags(textOrEmpty(request.tags()));
        article.setStatus(request.status());
    }

    private Account requireCurrentAccount(String email) {
        return accounts.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Vui long dang nhap lai."));
    }

    private Category requireCategory(Long id) {
        return categories.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Khong tim thay danh muc."));
    }

    private NewsArticle requireNews(Long id) {
        return news.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Khong tim thay bai viet."));
    }

    private NewsResponse newsResponse(NewsArticle article) {
        return new NewsResponse(article.getId(), article.getTitle(), article.getHeadline(), article.getContent(),
                article.getSource(), article.getCategory().getId(), article.getTags(), article.getStatus(),
                article.getCreatedBy().getId(), article.getCreatedDate());
    }

    private String textOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
