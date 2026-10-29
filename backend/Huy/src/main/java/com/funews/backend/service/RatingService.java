package com.funews.backend.service;

import com.funews.backend.dto.RatingRequest;
import com.funews.backend.dto.RatingResponse;
import com.funews.backend.entity.Account;
import com.funews.backend.entity.ArticleRating;
import com.funews.backend.entity.ArticleStatus;
import com.funews.backend.entity.NewsArticle;
import com.funews.backend.exception.ApiException;
import com.funews.backend.repository.AccountRepository;
import com.funews.backend.repository.ArticleRatingRepository;
import com.funews.backend.repository.NewsArticleRepository;
import com.funews.backend.security.AccountPrincipal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cham diem 1-5 sao.
 *
 * Khach vang lai doc bai va tim kiem duoc, nhung cham diem thi phai dang nhap -
 * do la ly do ton tai cua man hinh dang ky.
 *
 * HAI CHO PHAI DI CUNG NHAU trong mot transaction: dong trong article_ratings va
 * hai cot dem ratingCount / ratingSum tren bai viet. Lech mot cai la diem trung
 * binh sai vinh vien, vi rating_avg duoc database tinh tu hai cot dem do chu
 * khong tinh lai tu bang ratings.
 *
 * Moi nguoi chi co mot diem cho mot bai: cham lai la SUA dong cu, khong them
 * dong moi.
 */
@Service
@Transactional(readOnly = true)
public class RatingService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final NewsArticleRepository news;
    private final ArticleRatingRepository ratings;
    private final AccountRepository accounts;

    public RatingService(NewsArticleRepository news, ArticleRatingRepository ratings,
            AccountRepository accounts) {
        this.news = news;
        this.ratings = ratings;
        this.accounts = accounts;
    }

    /** Diem cua chinh nguoi dang dang nhap. score = null nghia la chua cham. */
    public RatingResponse myRating(Long articleId, AccountPrincipal viewer) {
        NewsArticle article = requireReadable(articleId);
        Long accountId = requireId(viewer);
        Integer score = ratings.findByArticleIdAndAccountIdAndDeletedFalse(articleId, accountId)
                .map(ArticleRating::getScore).orElse(null);
        return response(article, accountId, score);
    }

    @Transactional
    public RatingResponse rate(Long articleId, RatingRequest request, AccountPrincipal viewer) {
        NewsArticle article = requirePublished(articleId);
        Account me = requireAccount(viewer);
        int score = request.score();

        ArticleRating existing = ratings
                .findByArticleIdAndAccountIdAndDeletedFalse(articleId, me.getId()).orElse(null);
        if (existing == null) {
            ArticleRating rating = new ArticleRating();
            rating.setArticle(article);
            rating.setAccount(me);
            rating.setScore(score);
            ratings.save(rating);
            article.setRatingCount(article.getRatingCount() + 1);
            article.setRatingSum(article.getRatingSum() + score);
        } else {
            // Doi diem: bu tru vao tong, so luot giu nguyen.
            article.setRatingSum(article.getRatingSum() - existing.getScore() + score);
            existing.setScore(score);
            ratings.save(existing);
        }
        // saveAndFlush de database tinh lai rating_avg va doc ve ngay trong response.
        news.saveAndFlush(article);
        return response(article, me.getId(), score);
    }

    /** Go diem cua chinh minh. Xoa mem, va tru lai hai cot dem. */
    @Transactional
    public RatingResponse removeRating(Long articleId, AccountPrincipal viewer) {
        NewsArticle article = requireReadable(articleId);
        Account me = requireAccount(viewer);
        ArticleRating existing = ratings
                .findByArticleIdAndAccountIdAndDeletedFalse(articleId, me.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ban chua cham diem bai nay."));
        existing.setDeleted(true);
        existing.setDeletedAt(LocalDateTime.now(ZONE));
        ratings.save(existing);
        article.setRatingCount(Math.max(0, article.getRatingCount() - 1));
        article.setRatingSum(Math.max(0, article.getRatingSum() - existing.getScore()));
        news.saveAndFlush(article);
        return response(article, me.getId(), null);
    }

    private NewsArticle requirePublished(Long id) {
        NewsArticle article = requireReadable(id);
        if (article.getStatus() != ArticleStatus.PUBLISHED) {
            throw new ApiException(HttpStatus.CONFLICT, "Chi cham diem duoc bai dang cong khai.");
        }
        return article;
    }

    private NewsArticle requireReadable(Long id) {
        return news.findById(id).filter(article -> !article.isDeleted())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Khong tim thay bai viet."));
    }

    private Account requireAccount(AccountPrincipal viewer) {
        return accounts.findById(requireId(viewer))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Vui long dang nhap lai."));
    }

    private Long requireId(AccountPrincipal viewer) {
        if (viewer == null || viewer.id() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Vui long dang nhap de cham diem.");
        }
        return viewer.id();
    }

    private RatingResponse response(NewsArticle article, Long accountId, Integer score) {
        return new RatingResponse(article.getId(), accountId, score,
                article.getRatingCount(), article.getRatingAvg());
    }
}
